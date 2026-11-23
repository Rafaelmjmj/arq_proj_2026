# Roteiro de testes — Trokr, Entrega Final

Tudo pensado para o **terminal do Ubuntu**. Cada passo traz o comando, o que
deve aparecer, e o que significa se aparecer outra coisa. No fim há as
perguntas que o professor provavelmente fará, com a resposta curta e o
arquivo onde está o código.

> Nos exemplos, `RAIZ_1`, `RAIZ_2`, `ID_A` etc. são os ids que o
> `preparar-cenario.sh` imprime. Os seus números serão outros.

---

## 0. Preparar o terminal (uma vez)

Cole isto no terminal. Cria um atalho `api` que mostra a resposta **e** o
status HTTP de cada chamada:

```bash
B=http://localhost:8090
api() { curl -s -w '\n→ HTTP %{http_code}\n' -H 'Content-Type: application/json' "$@"; echo; }
```

---

## 1. Subir o sistema

```bash
cd ~/arq_proj_2026          # a pasta onde está o docker-compose.yml
docker compose down -v      # zera os bancos (a tabela item mudou na Aula 12)
docker compose up --build -d
docker compose ps
```

**Esperado:** três containers. `trokr-db` e `trokr-mongo` como `healthy`,
`trokr-app` como `running`/`Up`.

```bash
docker compose logs -f app       # Ctrl+C para sair
```

**Esperado:** a linha `Started TrokrApplication in X seconds`.

| Se aparecer | O que é | O que fazer |
|---|---|---|
| `permission denied ... docker.sock` | usuário sem permissão no Docker | `sudo` na frente dos comandos |
| `unknown command "compose"` | Docker antigo | usar `docker-compose` (com hífen) |
| `port is already allocated` | porta 5432, 27017 ou 8090 em uso | `docker ps` e parar o container antigo |
| `APPLICATION FAILED TO START` no log | erro na subida | copiar as últimas 40 linhas do log |
| app reiniciando em loop | banco antigo com a tabela velha | `docker compose down -v` e subir de novo |

```bash
api $B/actuator/health
```

**Esperado:** `{"status":"UP"}` e `→ HTTP 200`.

---

## 2. Swagger (Aula 14)

Abra no navegador: **http://localhost:8090/swagger-ui.html**

**Conferir:**
- título **Trokr API 1.0.0**, com a descrição sobre a RFC 9457;
- grupos em ordem alfabética: Avaliações, Demonstração, Histórico, Itens,
  Logs, Propostas, Usuários. O **Hello não aparece** (`@Hidden`);
- abrir `PATCH /propostas/{id}/finalizar`: a lista de respostas mostra
  **200, 404 e 409**, e o 409 com `application/problem+json` e o esquema
  `ProblemDetail`;
- abrir `POST /itens`: o exemplo do corpo já vem preenchido (`@Schema`).

```bash
api $B/v3/api-docs | head -c 300
```

**Esperado:** começa com `{"openapi":"3.1.0","info":{"title":"Trokr API"...`.

---

## 3. Cenário de teste

```bash
chmod +x preparar-cenario.sh corrida.sh
./preparar-cenario.sh
```

**Esperado** (os números mudam):

```
==> 1. Criando usuários A, B e C...
    Usuário A: id=1 | Usuário B: id=2 | Usuário C: id=3
==> 2. Criando itens com categorias diferentes...
    Item A (PRODUTO - 10 créditos): id=1
    Item B (SERVICO - 15 créditos): id=2
    Item C (EXPERIENCIA - 20 créditos): id=3
==> 3. Montando Negociação 1 ...
    Negociação 1 pronta em NEGOCIADO: Raiz=1 (Usuário A), Contraproposta=2 (Usuário B)
==> 4. Montando Negociação 2 ...
    Negociação 2 pronta em NEGOCIADO: Raiz=3 (Usuário C), Contraproposta=4 (Usuário A)
...
CENÁRIO PRONTO PARA A DEMONSTRAÇÃO DE CONCORRÊNCIA
```

Guarde os ids:

```bash
ID_A=1; ID_B=2; RAIZ_1=1; RAIZ_2=3      # troque pelos seus
```

Se der `Erro: POST /usuarios respondeu 000`: a aplicação ainda não subiu.
Espere o `Started TrokrApplication`.

---

## 4. Concorrência (Aula 10)

### 4.1 Religar a aplicação com o atraso de 2 s

```bash
TROKR_ATRASO_MS=2000 docker compose up -d
docker compose logs -f app      # esperar o Started, Ctrl+C
./preparar-cenario.sh           # cenário novo (o app reiniciou, mas os bancos ficaram)
ID_A=...; RAIZ_1=...; RAIZ_2=...   # atualize com os ids novos
```

### 4.2 Corrida 1 — a mesma proposta finalizada duas vezes ao mesmo tempo

```bash
./corrida.sh $RAIZ_1
```

**Esperado** (a ordem das linhas pode trocar):

```
req 1 (proposta 1): 200
req 2 (proposta 1): 409
```

Conferir que a troca **só aconteceu uma vez**:

```bash
api $B/usuarios/$ID_A                         # saldoCreditos = 10 (e não 20)
api "$B/avaliacoes?propostaId=$RAIZ_1"        # 2 avaliações PENDENTE (e não 4)
api "$B/logs?tipo=FINALIZACAO_RECUSADA"       # normalmente 1 aviso: a requisição que perdeu (AFTER_ROLLBACK)
```

| Se aparecer | Significa |
|---|---|
| `200` e `200` | o atraso não está ligado: refazer o 4.1 |
| `200` e `500` | o 409 não foi tratado (ver `GlobalExceptionHandler`) |
| saldo 20 | a troca rodou duas vezes |
| `FINALIZACAO_RECUSADA` vazio | não é falha: depende da ordem em que os listeners rodaram na requisição que perdeu. O que prova a correção é o saldo 10 e as 2 avaliações |

**Por quê:** as duas requisições leem a proposta na versão N. A primeira
grava e passa para N+1. A segunda tenta gravar com N, e o `@Version` recusa:
`OptimisticLockingFailureException` → **409
`PROPOSTA_ALTERADA_CONCORRENTEMENTE`**.

### 4.3 Corrida 2 — duas trocas diferentes do mesmo usuário ao mesmo tempo

```bash
./preparar-cenario.sh           # cenário novo: a corrida 1 já finalizou a Raiz 1
ID_A=...; RAIZ_1=...; RAIZ_2=...   # os ids novos
./corrida.sh $RAIZ_1 $RAIZ_2
api $B/usuarios/$ID_A
```

**Esperado:** `200` e `200`, e `"saldoCreditos":20` (10 + 10).

Se o saldo vier **10**, houve *lost update*: uma soma apagou a outra.
O UPDATE atômico (`saldo = saldo + :valor`) existe para isso não acontecer.

### 4.4 Desligar o atraso

```bash
docker compose up -d            # sem a variável: atraso volta a 0
```

---

## 5. Tratamento de exceções (Aula 11)

Todo erro volta como **ProblemDetail** (`Content-Type: application/problem+json`):

```json
{
  "type": "https://trokr.local/erros/usuario-nao-encontrado",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Usuário não encontrado com id 999",
  "instance": "/usuarios/999",
  "codigo": "USUARIO_NAO_ENCONTRADO",
  "timestamp": "2026-11-23T17:00:00Z"
}
```

`title` é fixo por tipo de erro, `detail` explica esta ocorrência, `codigo`
é estável para o programa cliente decidir o que fazer.

### 5.1 404 — RecursoNaoEncontradoException

```bash
api $B/usuarios/999
api $B/propostas/999
```

**Esperado:** `HTTP 404`, `codigo` `USUARIO_NAO_ENCONTRADO` e
`PROPOSTA_NAO_ENCONTRADA`.

### 5.2 409 — TransicaoInvalidaException (o State recusou)

```bash
./preparar-cenario.sh                                  # pegar uma RAIZ_1 nova
RAIZ_1=...                                             # o id novo
api -X PATCH $B/propostas/$RAIZ_1/finalizar            # 1ª vez
api -X PATCH $B/propostas/$RAIZ_1/finalizar            # 2ª vez
api -X PATCH $B/propostas/$RAIZ_1/aprovar              # aprovar algo Finalizado
```

**Esperado:**
1. `HTTP 200`, `"status":"FINALIZADO"`;
2. `HTTP 409`, `PROPOSTA_SEM_NEGOCIACAO`: "Nenhuma contraproposta em negociação para finalizar";
3. `HTTP 409`, `PROPOSTA_TRANSICAO_INVALIDA`: "Transição inválida: Finalizado → Ativa".

### 5.3 422 — RegraNegocioException (regra de negócio)

```bash
api -X POST $B/usuarios -d '{"nome":"Rafael","email":"rafa@trokr.dev","cidade":"Formosa"}'
api -X POST $B/usuarios -d '{"nome":"Outro","email":"rafa@trokr.dev","cidade":"Formosa"}'
```

**Esperado:** o 1º `HTTP 201`; o 2º `HTTP 422`, `EMAIL_JA_CADASTRADO`:
"Já existe um usuário com o e-mail rafa@trokr.dev".

### 5.4 400 — validação (`@Valid`) com a lista de campos

```bash
api -X POST $B/usuarios -d '{"nome":"","email":"isso-nao-e-email"}'
```

**Esperado:** `HTTP 400`, `codigo` `VALIDACAO` e:

```json
"campos": [
  {"campo": "nome",   "mensagem": "nome é obrigatório"},
  {"campo": "email",  "mensagem": "email deve ter um formato válido"},
  {"campo": "cidade", "mensagem": "cidade é obrigatório"}
]
```

(a ordem dos campos pode variar)

```bash
api $B/usuarios/abc
```

**Esperado:** `HTTP 400` em ProblemDetail, gerado pelo próprio Spring
(é isso que o `ResponseEntityExceptionHandler` nos dá de graça).

### 5.5 403, 400 e 409 na mesma avaliação

```bash
api "$B/avaliacoes?propostaId=$RAIZ_1"
```

Pegue o `id` de uma avaliação e o `usuarioAutorId` dela:

```bash
AV=1; AUTOR=1; OUTRO=3                         # troque pelos seus
api -X PATCH $B/avaliacoes/$AV -d "{\"usuarioId\":$OUTRO,\"nota\":5}"
api -X PATCH $B/avaliacoes/$AV -d "{\"usuarioId\":$AUTOR,\"nota\":9}"
api -X PATCH $B/avaliacoes/$AV -d "{\"usuarioId\":$AUTOR,\"nota\":5,\"descricao\":\"Troca ótima\"}"
api -X PATCH $B/avaliacoes/$AV -d "{\"usuarioId\":$AUTOR,\"nota\":4}"
```

**Esperado:**
1. `HTTP 403`, `AVALIACAO_AUTOR_INVALIDO` (não é o autor);
2. `HTTP 400`, `VALIDACAO`, campo `nota`: "nota máxima é 5";
3. `HTTP 200`, `"status":"AVALIADA"`;
4. `HTTP 409`, `AVALIACAO_JA_PREENCHIDA`.

### 5.6 500 — o catch-all

```bash
api $B/demo/erro-inesperado
```

**Esperado:** `HTTP 500`, `ERRO_INTERNO`, mensagem genérica e um campo
`ocorrencia` (um UUID). **Nunca** aparece o NullPointerException para o cliente.

```bash
docker compose logs app | grep ocorrencia      # o stack trace, com o MESMO UUID
api "$B/logs?tipo=ERRO_SISTEMA"                # o LogEvento no Mongo, com o MESMO UUID
```

---

## 6. Factory + Builder (Aula 12)

```bash
api -X POST $B/itens -d "{\"titulo\":\"Violão\",\"descricao\":\"Cordas novas\",\"categoria\":\"PRODUTO\",\"pesoGramas\":2500,\"usuarioId\":$ID_A}"
```

**Esperado:** `HTTP 201`, com `"categoria":"PRODUTO"` e `"pesoGramas":2500`,
**sem** `duracaoMinutos` nem `nivel` na resposta. Guarde o id: `ITEM=...`.

| Teste | Corpo (resumido) | Esperado |
|---|---|---|
| produto sem peso | `"categoria":"PRODUTO"` sem `pesoGramas` | `422 ITEM_DADOS_INCOMPLETOS` — "Um item da categoria PRODUTO precisa informar pesoGramas" |
| serviço com peso | `"categoria":"SERVICO","duracaoMinutos":30,"pesoGramas":200` | `422 ITEM_CAMPO_INCOMPATIVEL` — "Um item da categoria SERVICO não tem o campo pesoGramas" |
| experiência sem nível | `"categoria":"EXPERIENCIA"` | `422 ITEM_DADOS_INCOMPLETOS` |
| peso negativo | `"pesoGramas":-5` | `400 VALIDACAO` — "pesoGramas deve ser maior que zero" |
| produto virar serviço | `PUT /itens/$ITEM` com `"categoria":"SERVICO","duracaoMinutos":60` | `422 ITEM_CATEGORIA_IMUTAVEL` |

Comandos prontos:

```bash
api -X POST $B/itens -d "{\"titulo\":\"Livro\",\"descricao\":\"x\",\"categoria\":\"PRODUTO\",\"usuarioId\":$ID_A}"
api -X POST $B/itens -d "{\"titulo\":\"Conserto\",\"descricao\":\"x\",\"categoria\":\"SERVICO\",\"duracaoMinutos\":30,\"pesoGramas\":200,\"usuarioId\":$ID_A}"
api -X POST $B/itens -d "{\"titulo\":\"Trilha\",\"descricao\":\"x\",\"categoria\":\"EXPERIENCIA\",\"usuarioId\":$ID_A}"
api -X POST $B/itens -d "{\"titulo\":\"Caixa\",\"descricao\":\"x\",\"categoria\":\"PRODUTO\",\"pesoGramas\":-5,\"usuarioId\":$ID_A}"
api -X PUT  $B/itens/$ITEM -d "{\"titulo\":\"Violão\",\"descricao\":\"x\",\"categoria\":\"SERVICO\",\"duracaoMinutos\":60,\"usuarioId\":$ID_A}"
```

Ver a herança SINGLE_TABLE no banco:

```bash
docker exec -it trokr-db psql -U trokr_user -d trokr_db \
  -c "select id, categoria, titulo, peso_gramas, duracao_minutos, nivel from item;"
```

**Esperado:** uma tabela só; a coluna `categoria` é o discriminador; cada
linha só tem preenchida a coluna do seu tipo (as outras ficam vazias).

---

## 7. Tolerância a falhas: retry + disjuntor (Aula 13)

```bash
./preparar-cenario.sh                                   # duas negociações novas
RAIZ_1=...; RAIZ_2=...                                  # os ids impressos
api -X PATCH $B/demo/notificacao/fora-do-ar             # → HTTP 204
api -X PATCH $B/propostas/$RAIZ_1/finalizar             # → HTTP 200 (demora ~1 s)
api -X PATCH $B/propostas/$RAIZ_2/finalizar             # → HTTP 200 (rápido)
api "$B/logs?tipo=NOTIFICACAO_PENDENTE"                 # → 3 eventos
```

**O ponto principal:** com o serviço de notificação **morto**, as duas
finalizações respondem **200** e as trocas são concluídas.

No log da aplicação:

```bash
docker compose logs app | grep -E "Tentativa|Disjuntor|pendente|falhou"
```

**Esperado, nesta ordem** (no log aparecem os ids; aqui A, B e C são os usuários do cenário):

```
Tentativa 1 de 3 para o usuário A falhou; nova tentativa em 200 ms
Tentativa 2 de 3 para o usuário A falhou; nova tentativa em 400 ms
Notificação para o usuário A falhou                         ← 3 falhas: desistiu de A
Tentativa 1 de 3 para o usuário B falhou; nova tentativa em 200 ms
Disjuntor ABERTO até 2026-...                               ← 5ª falha seguida
Tentativa 2 de 3 para o usuário B falhou; nova tentativa em 400 ms
Disjuntor aberto: notificação para o usuário B ficou pendente
Disjuntor aberto: notificação para o usuário C ficou pendente   ← 2ª troca: nem tenta
Disjuntor aberto: notificação para o usuário A ficou pendente
```

Contagem: 5 chamadas ao serviço, **3 pendências** (B da 1ª troca, as duas da
2ª), **1 falha definitiva** (A da 1ª troca, em `GET /logs?tipo=ERRO_SISTEMA`).

Religar:

```bash
api -X PATCH $B/demo/notificacao/no-ar                  # → HTTP 204
sleep 30
./preparar-cenario.sh                                   # uma troca nova
api -X PATCH $B/propostas/RAIZ_NOVA/finalizar
docker compose logs app | grep -E "FECHADO|NOTIFICACAO ENVIADA" | tail -3
```

**Esperado:** `Disjuntor FECHADO: o serviço voltou` e
`[NOTIFICACAO ENVIADA] usuario=...`.

> O estado do disjuntor fica na memória: se reiniciar a aplicação, ele volta
> FECHADO. Se o teste não bater, rode `docker compose restart app` e repita
> a partir do `fora-do-ar`.

Retry sozinho, sem disjuntor (opcional):

```bash
TROKR_TAXA_FALHA=0.5 docker compose up -d     # metade das chamadas falha
```

Finalize uma troca e procure `Tentativa 1 de 3` no log. Volte com
`docker compose up -d`.

---

## 8. Conferir os dados nos bancos (se pedirem)

```bash
# MongoDB: o log de eventos
docker exec -it trokr-mongo mongosh -u trokr -p trokr123 --authenticationDatabase admin \
  --eval 'db.getSiblingDB("trokr").log_evento.aggregate([{$group:{_id:"$tipo",total:{$sum:1}}}])'

# Postgres: as tabelas do negócio
docker exec -it trokr-db psql -U trokr_user -d trokr_db -c "\dt"
docker exec -it trokr-db psql -U trokr_user -d trokr_db -c "select id, titulo, status, versao from proposta;"
```

A coluna `versao` é o `@Version`: cada gravação da proposta soma 1.

---

## 9. Desligar

```bash
docker compose down -v
```

---

## 10. Perguntas prováveis e respostas curtas

### Concorrência (Aula 10)

**Onde está o @Version e por que bloqueio otimista?**
`model/Proposta.java`, campo `versao`. Conflito na mesma proposta é raro
(são duas pessoas negociando); otimista não trava nada no caminho feliz, e
quem perde recebe 409. Decisão registrada no ADR 0002.

**Por que o @Transactional sozinho não resolve?**
Transação garante atomicidade (tudo ou nada), não exclusividade: duas
transações leem o mesmo dado ao mesmo tempo e as duas acham que podem gravar.

**Onde está o UPDATE atômico e por que não `usuario.setSaldo(saldo + x)`?**
`repository/UsuarioRepository.adicionarCreditos`. Ler, somar e gravar em
três passos deixa duas trocas lerem o mesmo saldo, e a última apaga a
primeira (lost update). O `saldo = saldo + :valor` faz o banco somar numa
operação só.

**Por que os créditos são aplicados em ordem de id?**
`listener/AtribuirCreditosListener`. Duas trocas travando os mesmos dois
usuários em ordens opostas podem se esperar para sempre (deadlock). Ordem
fixa evita.

**Por que AFTER_COMMIT no log e na notificação?**
`listener/RegistrarLogEventoListener` e `NotificarUsuariosListener`. Uma
mensagem enviada não volta atrás; se a transação desse rollback depois do
envio, o usuário seria avisado de uma troca que não aconteceu. Créditos,
avaliação e histórico ficam DENTRO da transação, porque precisam desfazer
junto.

### Exceções (Aula 11)

**Por que `sealed`?**
`exception/TrokrException.java`. O `switch` do `GlobalExceptionHandler`
conhece todas as subclasses: se alguém criar uma nova e esquecer de dar um
status a ela, o código não compila.

**Por que a 4ª subclasse, AcessoNegado (403)?**
Aceitar a contraproposta de outra pessoa não é regra violada (422) nem
estado errado (409): é falta de permissão.

**Por que o 500 não mostra a mensagem da exceção?**
Ela pode ter SQL, nome de tabela, caminho de arquivo. O cliente recebe um
código de ocorrência; quem opera acha o stack trace no log pelo mesmo código.

**Erro de cliente vai para o LogEvento?**
Não. Só o 500 (defeito do servidor) vira `ERRO_SISTEMA`. Um 404 é o cliente
pedindo algo que não existe, não um problema do sistema.

### Factory + Builder (Aula 12)

**Qual a diferença entre a fábrica e a Strategy de créditos?**
Mesma forma (interface + uma classe por categoria + mapa), intenção
diferente: a Strategy calcula sobre um objeto que já existe; a fábrica traz
o objeto à existência, válido.

**Onde o ItemService decide qual subclasse criar?**
Não decide: chama `CatalogoDeFabricas.criar`, que escolhe a fábrica pela
categoria num `Map`. Não há `switch` nem `new ItemProduto` no service.

**Por que não `instanceof ItemProduto`?**
Com `LAZY`, o Hibernate entrega um proxy de `Item`, e o `instanceof`
falha. O objeto responde: `getCategoria()` e `dadosDoTipo()` são polimórficos.

**Por que SINGLE_TABLE?** ADR 0004: uma tabela, sem JOIN, e a coluna
`categoria` que já existia virou o discriminador. Preço: as colunas de tipo
aceitam nulo no banco, e quem garante "produto tem peso" é a fábrica.

**Por que `@SuperBuilder` e não `@Builder`?** O `@Builder` não enxerga os
campos da classe mãe (`titulo`, `descricao`...). O `@SuperBuilder` enxerga.

**E um tipo novo (ITEM DIGITAL)?** Criar `ItemDigital`,
`FabricaItemDigital` e `CreditoDigital`; mudar só o enum e o DTO. Service,
catálogo, calculadora e Proposta ficam iguais.

### Tolerância a falhas (Aula 13)

**O que é a porta e o adaptador?** `NotificacaoPort` é o que o domínio
precisa ("avisar o usuário X"). `NotificacaoSimuladaAdapter` é como isso é
feito hoje. Trocar por e-mail ou SMS é escrever outro adaptador; o listener
não muda.

**Por que o disjuntor é um Decorator?** Implementa a mesma
`NotificacaoPort` que envolve. O listener não sabe que ele existe; ele entra
ou sai mudando só a `NotificacaoConfig`.

**Por que o disjuntor fica por DENTRO do retry?** Cada tentativa do retry
passa por ele e é contada; quando ele abre, devolve a pendência sem lançar e
o retry para na hora. Por fora, ele só veria a falha final de cada pessoa,
depois de todas as esperas.

**Quais os três estados?** Fechado (passa e conta falhas) → 5 falhas
seguidas → Aberto (nada passa, vira pendência) → 30 s → Meio-aberto (uma
chamada de teste): funcionou, fecha; falhou, abre de novo.

**Por que não Resilience4j?** ADR 0003: o objetivo era entender o mecanismo;
as anotações escondem a ordem das camadas.

**O que é o Proxy, e onde o Trokr usa?** Mesma forma do Decorator, mas
controla o acesso e é montado pelo Spring: repositories, `@Transactional`,
lazy loading. Armadilha: `this.metodo()` pula o proxy.

### Documentação (Aula 14)

**Por que o Swagger se chama "documentação viva"?** É gerado do próprio
código a cada subida: se o código muda, o Swagger muda junto, não fica
desatualizado.

**Por que declarar os erros em cada operação?** Para quem consome a API
saber, sem ler o código, que finalizar pode dar 409, e em que formato.

**Para que servem os ADRs?** Guardam o *porquê* de uma decisão e as
alternativas descartadas, que o código não mostra. Estão em `docs/adr/`.
