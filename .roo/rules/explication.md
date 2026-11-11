# Regra permanente — explicação didática ao final de toda tarefa

Este projeto é usado também para estudo. Código entregue sem explicação é considerada
tarefa incompleta.

## Gatilho obrigatório

Antes de chamar `attempt_completion`, verifique:

- A tarefa **criou, alterou, corrigiu, refatorou ou analisou** algum arquivo de código
  (`.java`, `.sql`, `.properties`, `pom.xml`, `docker-compose.yml`, configuração de
  infraestrutura)?

Se **sim**:

1. Carregue a skill `explication` com a ferramenta `skill`.
2. Siga o formato definido nela (completo ou enxuto, conforme o tamanho do diff).
3. Inclua a seção **"Explicação Didática"** no resultado final do `attempt_completion`,
   logo após o resumo técnico do que foi feito.

Se **não** (conversa, status do projeto, planejamento, dúvida conceitual):

- Não carregue a skill.
- Responda normalmente.

## Regras de execução

- Carregue a skill **uma única vez** por tarefa.
- Releia os arquivos no estado final antes de explicar; nunca explique de memória.
- A explicação não pode alterar código: apenas apontar problemas em "Pontos de atenção".
- Use links com número de linha, no formato [`Arquivo.java`](caminho/Arquivo.java:12).
- Idioma: português do Brasil, com termos técnicos em inglês preservados.

## Exceções explícitas do usuário

Se o usuário pedir "sem explicação", "resposta curta", "só o diff" ou "não quero a aula",
obedeça e pule esta etapa naquela tarefa. Na tarefa seguinte, o gatilho volta a valer.

## Escopo e prioridade

- Máximo de 5 arquivos em profundidade linha a linha.
- Ordem de prioridade: arquivos novos > arquivos alterados > arquivos apenas lidos.
- Se houver mais de 5 arquivos relevantes, explique os 5 principais e liste os demais em
  uma linha cada, oferecendo aprofundamento.
