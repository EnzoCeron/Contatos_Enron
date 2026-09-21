import java.io.IOException;

/**
 * Classe principal do projeto.
 *
 * Em vez de usar dados sintéticos, ela lê o conjunto real de e-mails extraído do ZIP
 * disponibilizado para a atividade e monta automaticamente o grafo de contatos.
 */
public class MainEnron {
    /**
     * Ponto de entrada da aplicação.
     *
     * O fluxo é: localizar o dataset, construir o grafo, calcular as métricas
     * gerais e executar os algoritmos solicitados no enunciado.
     * O primeiro argumento da linha de comando pode substituir o diretório padrão.
     */
    public static void main(String[] args) {
        try {
            String datasetPath = args.length > 0 ? args[0] : "Amostra Enron";
            EnronGraphAnalyzer analyzer = EnronGraphAnalyzer.fromDirectory(datasetPath);

            // Métricas gerais: quantidade de usuários, conexões e maiores graus.
            System.out.println("Vertices: " + analyzer.getNumberOfVertices());
            System.out.println("Edges: " + analyzer.getNumberOfEdges());
            System.out.println("Top 20 out-degree: " + analyzer.getTopOutDegree(20));
            System.out.println("Top 20 in-degree: " + analyzer.getTopInDegree(20));

            // Usuários escolhidos para demonstrar alcance, distância e caminho crítico.
            String source = "michelle.lokay@enron.com";
            String target = "steven.harris@enron.com";

            // DFS retorna um caminho encontrado explorando uma alternativa até o fim.
            System.out.println("DFS path " + source + " -> " + target + ": " + analyzer.depthFirstPath(source, target));

            // BFS retorna o caminho com menor número de arestas, quando ele existe.
            System.out.println("BFS path " + source + " -> " + target + ": " + analyzer.breadthFirstPath(source, target));

            // Distância considera apenas a quantidade de ligações, não o peso delas.
            System.out.println("Nodes at distance 2 from " + source + ": " + analyzer.nodesAtDistance(source, 2));

            // O caminho crítico usa a soma de 1/peso para representar dependência.
            System.out.println("Critical path " + source + " -> " + target + ": " + analyzer.criticalPathDetails(source, target));

            // As duas verificações abaixo mostram apenas se existe caminho.
            System.out.println("Reachability DFS: " + analyzer.canReachDepthFirst(source, target));
            System.out.println("Reachability BFS: " + analyzer.canReachBreadthFirst(source, target));
        } catch (IOException e) {
            // A leitura dos arquivos pode falhar por caminho inválido ou problema de acesso.
            System.err.println("Erro ao carregar o dataset do Enron: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
