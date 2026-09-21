# Definição de Projeto Colaborativo I
### Bacharelado em Ciência da Computação
### 5º Período – Resolução de Problemas com Grafos

**Data da entrega:** QUINTA-FEIRA dia 24/09/26. Não serão aceitos atrasos de horário e data.

## O que é necessário para a entrega

- Código Fonte em arquivo (enviado por atividade do AVA)
- Fazer teste de autoria no computador.

**Equipes:** até 4 pessoas

**Valor:** Complemento da Nota dos RAs 1 e 2 (40% de cada RA)

## Descrição do trabalho

O objetivo geral do trabalho consiste em desenvolver um analisador de contatos a partir da base de e-mails de benchmark conhecida como Enron Email Dataset. Essa base de e-mails é pública e está disponível em [https://www.cs.cmu.edu/~./enron/](https://www.cs.cmu.edu/~./enron/). Nesse projeto procura-se mostrar uma aplicação prática da teoria dos grafos que permite extrair informações úteis a partir da rede de contatos gerada com os e-mails da base.

## Requisitos e Funcionalidades do Analisador

1. **(Valor 2.0 Pontos)** A partir das mensagens de e-mail da base, gere um grafo direcionado considerando o remetente e o(s) destinatário(s) de cada mensagem. O grafo deve ser ponderado, considerando a frequência com que um remetente envia uma mensagem para um destinatário, e rotulado, considerando como rótulo o e-mail de cada usuário.

2. **(Valor 1.0 Ponto)** Implemente métodos/funções para extrair as seguintes informações gerais:
   - a. *(0.25 ponto)* O n. de vértices do grafo
   - b. *(0.25 ponto)* O n. de arestas do grafo
   - c. *(0.25 ponto)* Os 20 indivíduos que possuem maior grau de saída e o valor correspondente
   - d. *(0.25 ponto)* Os 20 indivíduos que possuem maior grau de entrada e o valor correspondente

3. **(Valor 1.5 Ponto)** Implemente um método/função que percorre o grafo em **PROFUNDIDADE** e verifica se um indivíduo X pode alcançar um indivíduo Y, retornando e mostrando o caminho percorrido (nós visitados) em uma lista.

4. **(Valor 1.5 Ponto)** Implemente um método/função que percorre o grafo em **LARGURA** e verifica se um indivíduo X pode alcançar um indivíduo Y, retornando e mostrando o caminho percorrido (nós visitados) em uma lista.

5. **(Valor 2.0 Pontos)** Implemente um método/função que retorne uma lista com os nós que estão a uma distância de D arestas de um nó N. Considere que uma ligação entre os nós X e Y corresponde a uma distância 1 entre X e Y.

6. **(Valor 2.0 Pontos)** Considerando que o peso das arestas denota um grau de dependência de um indivíduo A em relação a um indivíduo B, implemente um método/função que determine entre um indivíduo A e um indivíduo C o caminho crítico do fluxo de informação (maior custo acumulado) de forma aproximada, mostrando os indivíduos do caminho e a dependência acumulada no caminho (custos das arestas). Nesse caso, faça uma adaptação no algoritmo Dijkstra para que ele considere o inverso do peso das arestas (peso⁻¹).

## Observações

- Todos os algoritmos acima devem tratar o problema de ciclos (evitar loops).
- Programas prontos ou semi-prontos obtidos da Internet ou de outros colegas não serão considerados.
- Todas as equipes envolvidas em cópias integrais ou parciais de trabalhos (que forneceram ou que receberam as cópias) terão nota mínima (zero).
