# Analisador de Contatos Enron

Projeto desenvolvido para aplicar algoritmos de grafos sobre uma amostra real do **Enron Email Dataset**.

A aplicação lê os arquivos de e-mail, constrói um grafo direcionado e ponderado e executa as análises exigidas no enunciado.

## Estrutura final

- `EnronGraphAnalyzer.java`: contém o grafo, o parser dos e-mails e todos os algoritmos de análise.
- `MainEnron.java`: ponto de entrada; carrega o dataset e demonstra os resultados.
- `Amostra Enron/`: diretório com os arquivos reais utilizados na execução.
- `Analisador_de_Contatos_Enron_-_2026_atualizado.md`: documento original da atividade.

Não há uma classe artificial para representar mensagens. Cada arquivo real é lido diretamente pelo analisador.

## Modelo do grafo

O grafo é representado por uma lista de adjacência:

- cada endereço de e-mail é um vértice rotulado;
- cada mensagem cria uma aresta de `remetente -> destinatário`;
- o peso da aresta é a quantidade de mensagens enviadas entre os dois endereços;
- mensagens repetidas não criam novas arestas: apenas aumentam o peso existente;
- mensagens do usuário para ele mesmo são ignoradas;
- as estruturas `visited` e `settled` impedem loops durante as buscas.

Exemplo conceitual:

```text
alice@enron.com --(3)--> bob@enron.com
```

Nesse caso, Alice enviou três mensagens para Bob. O número de arestas conta a conexão distinta, enquanto o grau soma os pesos das conexões.

## Fluxo de execução

1. `MainEnron.main` define o diretório do dataset. O caminho padrão é `Amostra Enron`, mas o primeiro argumento da execução pode substituí-lo.
2. `EnronGraphAnalyzer.fromDirectory` percorre o diretório e suas subpastas com `Files.walk`.
3. Cada arquivo é enviado para `addMessageFromFile`.
4. O parser lê os cabeçalhos `From`, `To`, `Cc` e `Bcc`.
5. Os endereços são extraídos por expressão regular, normalizados para letras minúsculas e retirados de duplicidades.
6. `addEmail` cria os vértices e insere ou atualiza as arestas direcionadas.
7. Depois que todos os arquivos foram processados, o programa executa as métricas gerais e os algoritmos pedidos.

## Principais funções

### Carregamento e construção

- `fromDirectory(String rootDirectory)`: cria o analisador e carrega todos os arquivos do diretório.
- `addMessageFromFile(Path filePath)`: lê um arquivo real, extrai remetente e destinatários e adiciona suas conexões.
- `extractHeaderValue(...)`: lê um cabeçalho e suas linhas continuadas.
- `parseEmails(...)`: encontra todos os endereços contidos em um texto.
- `normalizeEmail(...)`: padroniza o endereço para evitar que o mesmo usuário seja cadastrado mais de uma vez.
- `addEmail(...)`: transforma uma mensagem em arestas do grafo.

### Métricas do grafo

- `getNumberOfVertices()`: retorna a quantidade de endereços distintos.
- `getNumberOfEdges()`: retorna a quantidade de conexões distintas.
- `getOutDegree(...)`: soma os pesos das arestas que saem de um usuário.
- `getInDegree(...)`: soma os pesos das arestas que chegam a um usuário.
- `getTopOutDegree(20)`: retorna os 20 maiores graus de saída.
- `getTopInDegree(20)`: retorna os 20 maiores graus de entrada.

### Busca em profundidade

`depthFirstPath(source, target)` usa DFS recursiva:

1. marca o vértice atual como visitado;
2. adiciona o vértice ao caminho;
3. visita cada vizinho ainda não visitado;
4. se um ramo não chegar ao destino, remove o último vértice e tenta outro ramo.

A marcação de visitados evita que ciclos façam a busca entrar em repetição infinita.

### Busca em largura

`breadthFirstPath(source, target)` usa BFS com uma fila:

1. insere a origem na fila;
2. retira um vértice por vez;
3. insere seus vizinhos ainda não visitados;
4. guarda o predecessor de cada vizinho;
5. ao encontrar o destino, reconstrói o caminho pelos predecessores.

Como os vértices são visitados camada por camada, o caminho retornado possui o menor número de arestas.

### Nós a uma distância D

`nodesAtDistance(source, distance)` também usa BFS. Além da fila, mantém um mapa com a distância mínima de cada vértice em relação à origem. Ao final, retorna apenas os vértices cuja distância é exatamente `D`.

O peso da aresta não altera essa distância: cada ligação conta como uma aresta de distância 1, conforme o enunciado.

### Caminho crítico aproximado

`criticalPathDetails(start, end)` usa uma adaptação de Dijkstra.

Para uma aresta de peso `w`, o custo utilizado é:

```text
custo = 1 / w
```

O algoritmo acumula esses custos e prioriza o maior valor acumulado. Assim, o caminho retornado representa uma aproximação do maior custo de dependência entre os usuários.

A fila de prioridade organiza os candidatos. O vetor `parent` permite reconstruir o caminho final, enquanto `settled` impede reprocessamento e ciclos.

> Observação: essa interpretação segue literalmente o enunciado, que solicita o uso do inverso do peso das arestas. Em uma interpretação de menor custo, Dijkstra normalmente minimizaria a soma; aqui o objetivo da atividade é mostrar o maior custo acumulado.

## Como compilar e executar

Abra o terminal dentro desta pasta e execute:

```bash
javac MainEnron.java EnronGraphAnalyzer.java
java MainEnron
```

Para usar outro diretório com arquivos de e-mail:

```bash
java MainEnron "caminho/do/dataset"
```

O diretório padrão `Amostra Enron` deve permanecer no mesmo local dos arquivos `.java` quando nenhum argumento for informado.

## Resultado validado

Na execução com a amostra fornecida, o programa constrói um grafo com aproximadamente:

- 6320 vértices;
- 17837 arestas distintas.

Os valores podem mudar caso o conteúdo do dataset seja substituído ou ampliado.

## Relação com o enunciado

| Requisito | Implementação |
|---|---|
| Grafo direcionado, ponderado e rotulado | `addMessageFromFile`, `addEmail`, lista de adjacência e pesos acumulados |
| Número de vértices e arestas | `getNumberOfVertices`, `getNumberOfEdges` |
| 20 maiores graus de saída e entrada | `getTopOutDegree(20)`, `getTopInDegree(20)` |
| Alcance em profundidade | `depthFirstPath` e `canReachDepthFirst` |
| Alcance em largura | `breadthFirstPath` e `canReachBreadthFirst` |
| Nós a distância D | `nodesAtDistance` |
| Caminho crítico aproximado | `criticalPath`, `criticalPathCost` e `criticalPathDetails` |
| Tratamento de ciclos | `visited` nas buscas e `settled` no caminho crítico |
