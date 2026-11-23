# 0003. Escrever o retry e o disjuntor à mão, como Decorators da NotificacaoPort

## Status
Aceito (Aula 13).

## Contexto
O serviço de notificação é uma dependência acessória: a troca vale sem ele.
Com o retry da Aula 11 dentro do listener, um serviço fora do ar custava
~2 s e 6 chamadas inúteis por finalização, na thread da requisição. Com
finalizações chegando mais rápido do que as threads se liberam, até um
`GET /usuarios` deixaria de responder: falha em cascata. Precisamos parar de
insistir quando o serviço caiu, sem perder as notificações e sem espalhar
essa lógica pelo listener.

## Alternativas consideradas
- **Resilience4j** (`@Retry` e `@CircuitBreaker`): pronto, testado, com
  métricas e mais estados. Perdeu porque o objetivo da disciplina é entender
  o mecanismo; as anotações funcionam via proxy do Spring e escondem a ordem
  das camadas, que é justamente o que precisa ser explicado; e é mais uma
  dependência e mais configuração para um único ponto de integração.
- **Manter o retry no listener e só baixar as tentativas**: menos espera,
  mas o serviço morto continua recebendo chamadas em toda finalização.

## Decisão
O listener depende só da porta `NotificacaoPort`. Retry
(`NotificacaoComRetry`) e disjuntor (`NotificacaoComDisjuntor`) são
Decorators da mesma porta, montados na `NotificacaoConfig` com o disjuntor
por dentro do retry. Com o disjuntor aberto, a notificação vira
`NOTIFICACAO_PENDENTE` no log.

## Consequências
- \+ Cada proteção é uma classe pequena, testada isolada e com relógio falso.
- \+ Trocar o provedor de notificação é escrever outro adaptador da porta.
- \- O estado do disjuntor fica na memória da instância: com duas instâncias,
  cada uma tem o seu, e ele se perde ao reiniciar.
- \- O `synchronized` serializa as notificações; aceitável com a carga do
  Trokr, não com muitas notificações por segundo.
- \- Pendências ficam só registradas: ainda não há um reenvio automático.
  Se a necessidade crescer (métricas, bulkhead, vários serviços), migrar
  para o Resilience4j vira um ADR que substitui este.
