# 0002. Usar bloqueio otimista com @Version na Proposta

## Status
Aceito (Aula 10).

## Contexto
Duas requisições podem finalizar a mesma Proposta ao mesmo tempo (dois
cliques, duas abas, um retry do front-end). Sem controle, as duas leem a
proposta em NEGOCIADO, as duas passam pelo State e as duas publicam
`TrocaConcluidaEvent`: créditos em dobro, avaliações em dobro. Ao mesmo
tempo, conflitos na mesma proposta são raros: são duas pessoas negociando,
não milhares disputando o mesmo registro.

## Alternativas consideradas
- **Bloqueio pessimista** (`SELECT ... FOR UPDATE`): a segunda requisição
  espera a primeira terminar. Correto, mas segura uma linha travada durante
  toda a finalização (que inclui os listeners), e cria risco de deadlock e de
  tempo-limite, para proteger um conflito que quase nunca acontece.
- **`synchronized` no service**: só funciona com uma instância da aplicação,
  e serializa todas as finalizações, inclusive as de propostas diferentes.

## Decisão
A Proposta tem um campo `@Version`. Quem grava com versão desatualizada
perde: o `GlobalExceptionHandler` responde 409
`PROPOSTA_ALTERADA_CONCORRENTEMENTE`. O saldo de créditos, que é somado por
várias trocas, usa um UPDATE atômico (`saldo = saldo + :valor`).

## Consequências
- \+ Nenhuma trava no caminho feliz; o conflito raro vira uma resposta clara.
- \+ O teste com o `corrida.sh` mostra um 200 e um 409, e o saldo correto.
- \- Quem perde a corrida recebe erro e precisa recarregar; o servidor não
  tenta de novo sozinho (a repetição veria a proposta já Finalizada).
- \- Se um dia houver muita disputa pela mesma linha, quase toda tentativa
  vai falhar com conflito, e este ADR precisa ser revisto.
