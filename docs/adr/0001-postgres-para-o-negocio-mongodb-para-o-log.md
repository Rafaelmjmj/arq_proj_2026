# 0001. Usar Postgres para o negócio e MongoDB para o log de eventos

## Status
Aceito (Aula 8).

## Contexto
Usuário, Item, Proposta, Avaliação e Histórico são dados relacionados entre
si (chaves estrangeiras, transições de estado que precisam de transação e,
desde a Aula 10, `@Version` e UPDATE atômico do saldo). O log de eventos é
outra coisa: cresce a cada ação, nunca é alterado, é só consultado por tipo
ou por período, e cada tipo de evento carrega um payload com campos
diferentes (`TROCA_CONCLUIDA` tem itens e créditos; `NOTIFICACAO_PENDENTE`
tem usuário e mensagem; `ERRO_SISTEMA` tem a exceção e a ocorrência).

## Alternativas consideradas
- **Tudo no Postgres**, com uma tabela `log_evento` e o payload em `jsonb`:
  um banco a menos para operar, mas o log disputaria espaço, índices e
  vacuum com as tabelas do negócio, e o payload variável ficaria num campo
  que o modelo relacional não ajuda a consultar.
- **Arquivo de log da aplicação** (só `log.info`): zero infraestrutura, mas
  não dá para consultar por tipo pela API (`GET /logs?tipo=...`), e o log se
  perde quando o container é recriado.

## Decisão
O negócio fica no Postgres (JPA). O log de eventos vai para o MongoDB, atrás
da porta `RegistroDeEventos`, com `RegistroDeEventosMongo` como adaptador.

## Consequências
- \+ Cada evento guarda o payload que faz sentido para ele, sem migração.
- \+ O negócio não conhece o Mongo: o `LogEventoService` depende só da porta,
  e os testes usam um registro em memória no lugar.
- \- Dois bancos para subir, monitorar e fazer backup (o `docker-compose.yml`
  tem os dois, com healthcheck).
- \- Não há transação entre os dois: um log pode ser gravado para uma
  operação que depois sofre rollback. Por isso os listeners de log rodam em
  `AFTER_COMMIT` (Aula 10), e uma falha ao gravar o log nunca derruba a
  operação principal.
