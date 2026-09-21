import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Classe principal do projeto: analisa a rede de comunicação entre e-mails reais do Enron.
 *
 * Cada pessoa é representada como um vértice e cada mensagem enviada para um destinatário
 * vira uma aresta direcionada. O peso da aresta representa quantas vezes essa comunicação
 * ocorreu, tornando o grafo ponderado e adequado para métricas de centralidade e alcance.
 */
public class EnronGraphAnalyzer {
    // Expressão usada para localizar endereços em cabeçalhos simples ou compostos.
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // Relaciona cada e-mail ao índice usado internamente no grafo.
    private final Map<String, Integer> vertexIndexByEmail;

    // Guarda os rótulos dos vértices; emails.get(i) identifica o vértice i.
    private final List<String> emails;

    // Lista de adjacência: cada posição contém as arestas que saem daquele vértice.
    private final List<List<Edge>> adjacencyList;

    // Conta conexões distintas; mensagens repetidas aumentam o peso da aresta.
    private int edgeCount;

    /** Representa uma aresta direcionada e a frequência da comunicação. */
    private static class Edge {
        final int to;
        int weight;

        /** Cria uma aresta apontando para o destino com a frequência inicial informada. */
        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    /** Inicializa um grafo vazio antes do carregamento dos arquivos. */
    public EnronGraphAnalyzer() {
        this.vertexIndexByEmail = new HashMap<>();
        this.emails = new ArrayList<>();
        this.adjacencyList = new ArrayList<>();
        this.edgeCount = 0;
    }

    /**
     * Carrega automaticamente todos os arquivos reais do diretório do Enron.
     */
    public static EnronGraphAnalyzer fromDirectory(String rootDirectory) throws IOException {
        EnronGraphAnalyzer analyzer = new EnronGraphAnalyzer();
        Path root = Paths.get(rootDirectory);

        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Diretório do Enron não encontrado: " + rootDirectory);
        }

        // Files.walk percorre também as subpastas de cada usuário, como inbox e outbox.
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().startsWith("._"))
                    .forEach(path -> {
                        try {
                            analyzer.addMessageFromFile(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        return analyzer;
    }

    /**
     * Lê um e-mail real do dataset e transforma em uma entrada do grafo.
     */
    public void addMessageFromFile(Path filePath) throws IOException {
        // ISO-8859-1 preserva os bytes do dataset original sem depender do locale da máquina.
        String content = Files.readString(filePath, StandardCharsets.ISO_8859_1);

        // Os destinatários podem estar em To, Cc ou Bcc; todos geram conexões.
        String sender = extractHeaderValue(content, "From");
        String recipientsText = extractHeaderValue(content, "To") + " "
                + extractHeaderValue(content, "Cc") + " "
                + extractHeaderValue(content, "Bcc");

        // Alguns arquivos antigos não possuem From utilizável, mas possuem X-From.
        if (sender == null || sender.isBlank()) {
            sender = extractHeaderValue(content, "X-From");
        }

        if (sender == null || sender.isBlank()) {
            return;
        }

        Set<String> senderSet = parseEmails(sender);
        Set<String> recipients = parseEmails(recipientsText);

        // Mensagens sem remetente ou destinatário não formam aresta e são ignoradas.
        if (senderSet.isEmpty() || recipients.isEmpty()) {
            return;
        }

        String senderEmail = senderSet.iterator().next();
        addEmail(senderEmail, new ArrayList<>(recipients));
    }

    /**
     * Adiciona uma ligação do remetente para cada destinatário.
     */
    public void addEmail(String sender, List<String> recipients) {
        if (sender == null || sender.isBlank()) {
            return;
        }

        String normalizedSender = normalizeEmail(sender);
        if (normalizedSender == null) {
            return;
        }

        // O conjunto elimina duplicidade quando o mesmo endereço aparece em To e Cc.
        int senderIndex = getOrCreateVertex(normalizedSender);
        Set<String> uniqueRecipients = new LinkedHashSet<>();
        for (String recipient : recipients) {
            String normalized = normalizeEmail(recipient);
            if (normalized != null) {
                uniqueRecipients.add(normalized);
            }
        }

        // Cada destinatário cria ou atualiza uma aresta remetente -> destinatário.
        for (String recipient : uniqueRecipients) {
            int recipientIndex = getOrCreateVertex(recipient);
            addWeightedEdge(senderIndex, recipientIndex, 1);
        }
    }

    /** Limpa um valor e retorna seu endereço em minúsculas, ou null se for inválido. */
    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String cleaned = email.trim().replaceAll("^[<\"']+|[>\"']+$", "");
        Matcher matcher = EMAIL_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /** Extrai todos os endereços encontrados em um texto, sem repeti-los. */
    private static Set<String> parseEmails(String text) {
        Set<String> emailsSet = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return emailsSet;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = matcher.group().trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) {
                emailsSet.add(value);
            }
        }
        return emailsSet;
    }

    /**
     * Lê um cabeçalho RFC simples e suas linhas continuadas.
     * A leitura para na primeira linha vazia, pois dali em diante começa o corpo.
     */
    private static String extractHeaderValue(String content, String headerName) {
        String[] lines = content.split("\\r?\\n");
        StringBuilder buffer = new StringBuilder();
        boolean capturing = false;

        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }

            if (line.regionMatches(true, 0, headerName + ":", 0, headerName.length() + 1)) {
                buffer.append(line.substring(headerName.length() + 1).trim()).append(" ");
                capturing = true;
                continue;
            }

            if (capturing) {
                if (Character.isWhitespace(line.charAt(0))) {
                    buffer.append(line.trim()).append(" ");
                    continue;
                }
                break;
            }
        }

        String result = buffer.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /** Retorna o índice do e-mail e cria o vértice quando ele aparece pela primeira vez. */
    private int getOrCreateVertex(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!vertexIndexByEmail.containsKey(normalized)) {
            int index = emails.size();
            vertexIndexByEmail.put(normalized, index);
            emails.add(normalized);
            adjacencyList.add(new ArrayList<>());
        }
        return vertexIndexByEmail.get(normalized);
    }

    /**
     * Insere uma conexão nova ou acumula sua frequência em uma conexão existente.
     * Laços para o próprio usuário são descartados porque não representam contato entre indivíduos.
     */
    private void addWeightedEdge(int from, int to, int weight) {
        if (from == to || weight <= 0) {
            return;
        }

        for (Edge edge : adjacencyList.get(from)) {
            if (edge.to == to) {
                edge.weight += weight;
                return;
            }
        }

        adjacencyList.get(from).add(new Edge(to, weight));
        edgeCount++;
    }

    /** Retorna o número de usuários distintos encontrados nos arquivos. */
    public int getNumberOfVertices() {
        return emails.size();
    }

    /** Retorna o número de conexões distintas, sem somar seus pesos. */
    public int getNumberOfEdges() {
        return edgeCount;
    }

    /** Retorna os rótulos dos vértices em ordem de criação, sem permitir alteração externa. */
    public List<String> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    /** Localiza o índice de um e-mail; -1 indica que ele não está no grafo. */
    public int getVertexIndex(String email) {
        if (email == null) {
            return -1;
        }
        return vertexIndexByEmail.getOrDefault(email.trim().toLowerCase(Locale.ROOT), -1);
    }

    /** Converte um índice interno de volta para o rótulo do vértice. */
    public String getVertexEmail(int vertex) {
        if (!isValidIndex(vertex)) {
            throw new IllegalArgumentException("Invalid vertex index: " + vertex);
        }
        return emails.get(vertex);
    }

    /** Soma os pesos das arestas que saem do usuário. */
    public int getOutDegree(String email) {
        int vertex = getVertexIndex(email);
        if (vertex == -1) {
            throw new IllegalArgumentException("Email not found in the graph: " + email);
        }

        int total = 0;
        for (Edge edge : adjacencyList.get(vertex)) {
            total += edge.weight;
        }
        return total;
    }

    /** Soma os pesos das arestas que chegam ao usuário. */
    public int getInDegree(String email) {
        int target = getVertexIndex(email);
        if (target == -1) {
            throw new IllegalArgumentException("Email not found in the graph: " + email);
        }

        int total = 0;
        for (List<Edge> vertexEdges : adjacencyList) {
            for (Edge edge : vertexEdges) {
                if (edge.to == target) {
                    total += edge.weight;
                }
            }
        }
        return total;
    }

    /** Ordena os usuários pelo maior grau de saída e retorna nome e valor. */
    public List<String> getTopOutDegree(int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<VertexMetric> metrics = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            metrics.add(new VertexMetric(emails.get(i), getOutDegree(emails.get(i))));
        }
        metrics.sort(Comparator.comparingInt(VertexMetric::getValue).reversed().thenComparing(VertexMetric::getName));

        List<String> result = new ArrayList<>();
        int count = Math.min(limit, metrics.size());
        for (int i = 0; i < count; i++) {
            result.add(metrics.get(i).getName() + " (" + metrics.get(i).getValue() + ")");
        }
        return result;
    }

    /** Ordena os usuários pelo maior grau de entrada e retorna nome e valor. */
    public List<String> getTopInDegree(int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<VertexMetric> metrics = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            metrics.add(new VertexMetric(emails.get(i), getInDegree(emails.get(i))));
        }
        metrics.sort(Comparator.comparingInt(VertexMetric::getValue).reversed().thenComparing(VertexMetric::getName));

        List<String> result = new ArrayList<>();
        int count = Math.min(limit, metrics.size());
        for (int i = 0; i < count; i++) {
            result.add(metrics.get(i).getName() + " (" + metrics.get(i).getValue() + ")");
        }
        return result;
    }

    /** Executa DFS e reduz seu resultado à resposta booleana de alcançabilidade. */
    public boolean canReachDepthFirst(String source, String target) {
        return !depthFirstPath(source, target).isEmpty();
    }

    /** Retorna um caminho encontrado por busca em profundidade, ou lista vazia. */
    public List<String> depthFirstPath(String source, String target) {
        int sourceIndex = getVertexIndex(source);
        int targetIndex = getVertexIndex(target);
        if (sourceIndex == -1 || targetIndex == -1) {
            throw new IllegalArgumentException("Source or target not found in the graph.");
        }

        boolean[] visited = new boolean[emails.size()];
        List<String> path = new ArrayList<>();
        if (dfs(sourceIndex, targetIndex, visited, path)) {
            return path;
        }
        return new ArrayList<>();
    }

    /** Expande recursivamente a DFS, removendo do caminho os ramos que falharam. */
    private boolean dfs(int current, int target, boolean[] visited, List<String> path) {
        visited[current] = true;
        path.add(emails.get(current));

        if (current == target) {
            return true;
        }

        for (Edge edge : adjacencyList.get(current)) {
            if (!visited[edge.to] && dfs(edge.to, target, visited, path)) {
                return true;
            }
        }

        path.remove(path.size() - 1);
        return false;
    }

    /** Executa BFS e reduz seu resultado à resposta booleana de alcançabilidade. */
    public boolean canReachBreadthFirst(String source, String target) {
        return !breadthFirstPath(source, target).isEmpty();
    }

    /** Retorna o caminho com menor número de arestas encontrado pela BFS. */
    public List<String> breadthFirstPath(String source, String target) {
        int sourceIndex = getVertexIndex(source);
        int targetIndex = getVertexIndex(target);
        if (sourceIndex == -1 || targetIndex == -1) {
            throw new IllegalArgumentException("Source or target not found in the graph.");
        }

        if (sourceIndex == targetIndex) {
            return new ArrayList<>(Collections.singletonList(source));
        }

        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[emails.size()];
        int[] parent = new int[emails.size()];
        Arrays.fill(parent, -1);

        queue.add(sourceIndex);
        visited[sourceIndex] = true;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (Edge edge : adjacencyList.get(current)) {
                int next = edge.to;
                if (!visited[next]) {
                    visited[next] = true;
                    parent[next] = current;
                    queue.add(next);
                    if (next == targetIndex) {
                        return reconstructPath(parent, sourceIndex, targetIndex);
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    /** Reconstrói o caminho da BFS seguindo os predecessores até a origem. */
    private List<String> reconstructPath(int[] parent, int sourceIndex, int targetIndex) {
        LinkedList<String> path = new LinkedList<>();
        int current = targetIndex;
        while (current != -1) {
            path.addFirst(emails.get(current));
            if (current == sourceIndex) {
                break;
            }
            current = parent[current];
        }
        return new ArrayList<>(path);
    }

    /** Retorna exatamente os vértices cuja menor distância em arestas é D. */
    public List<String> nodesAtDistance(String source, int distance) {
        int sourceIndex = getVertexIndex(source);
        if (sourceIndex == -1) {
            throw new IllegalArgumentException("Source not found in the graph.");
        }
        if (distance < 0) {
            throw new IllegalArgumentException("Distance must be non-negative.");
        }

        if (distance == 0) {
            return new ArrayList<>(Collections.singletonList(source));
        }

        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distances = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(sourceIndex);
        visited.add(sourceIndex);
        distances.put(sourceIndex, 0);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentDistance = distances.get(current);
            for (Edge edge : adjacencyList.get(current)) {
                int next = edge.to;
                if (!visited.contains(next)) {
                    visited.add(next);
                    distances.put(next, currentDistance + 1);
                    queue.add(next);
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : distances.entrySet()) {
            if (entry.getValue() == distance) {
                result.add(emails.get(entry.getKey()));
            }
        }
        Collections.sort(result);
        return result;
    }

    /** Retorna somente os e-mails do caminho crítico aproximado. */
    public List<String> criticalPath(String start, String end) {
        return criticalPathResult(start, end).path;
    }

    /** Retorna a soma dos custos inversos das arestas do caminho crítico. */
    public double criticalPathCost(String start, String end) {
        return criticalPathResult(start, end).cost;
    }

    /** Formata o caminho crítico e sua dependência acumulada para exibição. */
    public String criticalPathDetails(String start, String end) {
        CriticalPathResult result = criticalPathResult(start, end);
        if (result.path.isEmpty()) {
            return "No path from " + start + " to " + end + ".";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Critical path: ");
        for (int i = 0; i < result.path.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(result.path.get(i));
        }
        builder.append(" | accumulated dependence = ").append(String.format("%.4f", result.cost));
        return builder.toString();
    }

    /**
     * Adapta Dijkstra para maximizar a soma de 1/peso.
     * A fila prioriza a maior dependência acumulada e 'settled' impede ciclos e revisitas.
     */
    private CriticalPathResult criticalPathResult(String start, String end) {
        int source = getVertexIndex(start);
        int target = getVertexIndex(end);
        if (source == -1 || target == -1) {
            throw new IllegalArgumentException("Start or end email not found in the graph.");
        }

        double[] best = new double[emails.size()];
        Arrays.fill(best, Double.NEGATIVE_INFINITY);
        boolean[] settled = new boolean[emails.size()];
        int[] parent = new int[emails.size()];
        Arrays.fill(parent, -1);
        double[] edgeContribution = new double[emails.size()];
        Arrays.fill(edgeContribution, 0.0);

        best[source] = 0.0;
        PriorityQueue<WeightedVertex> pq = new PriorityQueue<>(Comparator.comparingDouble(v -> -v.score));
        pq.add(new WeightedVertex(source, 0.0));

        while (!pq.isEmpty()) {
            WeightedVertex current = pq.poll();
            if (settled[current.vertex]) {
                continue;
            }
            settled[current.vertex] = true;

            if (current.score < best[current.vertex] - 1e-9) {
                continue;
            }

            if (current.vertex == target) {
                break;
            }

            for (Edge edge : adjacencyList.get(current.vertex)) {
                double contribution = 1.0 / (double) edge.weight;
                double candidate = best[current.vertex] + contribution;
                if (candidate > best[edge.to] + 1e-9) {
                    best[edge.to] = candidate;
                    parent[edge.to] = current.vertex;
                    edgeContribution[edge.to] = contribution;
                    pq.add(new WeightedVertex(edge.to, candidate));
                }
            }
        }

        List<String> path = new ArrayList<>();
        double totalCost = 0.0;
        if (best[target] != Double.NEGATIVE_INFINITY) {
            int current = target;
            while (current != -1) {
                path.add(0, emails.get(current));
                totalCost += edgeContribution[current];
                if (current == source) {
                    break;
                }
                current = parent[current];
            }
        }

        return new CriticalPathResult(path, totalCost);
    }

    /** Verifica se um índice pode ser usado na lista de vértices. */
    private boolean isValidIndex(int index) {
        return index >= 0 && index < emails.size();
    }

    /** Par usado para ordenar um e-mail junto com seu grau. */
    private static class VertexMetric {
        private final String name;
        private final int value;

        /** Guarda o rótulo e o valor usados na ordenação das métricas. */
        VertexMetric(String name, int value) {
            this.name = name;
            this.value = value;
        }

        /** Retorna o e-mail associado à métrica. */
        public String getName() {
            return name;
        }

        /** Retorna o grau associado ao e-mail. */
        public int getValue() {
            return value;
        }
    }

    /** Estado colocado na fila de prioridade do caminho crítico. */
    private static class WeightedVertex {
        final int vertex;
        final double score;

        /** Cria um candidato com sua pontuação acumulada na fila de prioridade. */
        WeightedVertex(int vertex, double score) {
            this.vertex = vertex;
            this.score = score;
        }
    }

    /** Agrupa caminho e custo para evitar recalcular a busca. */
    private static class CriticalPathResult {
        final List<String> path;
        final double cost;

        /** Agrupa o caminho encontrado e a soma dos custos das suas arestas. */
        CriticalPathResult(List<String> path, double cost) {
            this.path = path;
            this.cost = cost;
        }
    }

    /** Imprime um resumo reutilizável das principais métricas do grafo. */
    public void printSummary() {
        System.out.println("Graph summary:");
        System.out.println("Vertices: " + getNumberOfVertices());
        System.out.println("Edges: " + getNumberOfEdges());
        System.out.println("Top out-degree emails (max 20):");
        for (String entry : getTopOutDegree(20)) {
            System.out.println("  - " + entry);
        }
        System.out.println("Top in-degree emails (max 20):");
        for (String entry : getTopInDegree(20)) {
            System.out.println("  - " + entry);
        }
    }
}
