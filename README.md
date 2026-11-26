# Trokr

Plataforma de trocas colaborativas: usuários cadastram itens ou habilidades
que oferecem e propõem trocas entre si, sem dinheiro envolvido.

> **"Trokr" é um nome de trabalho.** A turma vai batizar o projeto na Aula 1.
> O nome só aparece em lugares fáceis de trocar depois (pacote Java, nome do
> projeto Maven, nomes de containers/banco no Docker) — não está espalhado
> pela lógica de negócio.

## Roteiro de Testes do professor (Entrega Final)

O professor distribuiu o **Roteiro de Testes de Aceitação do Trokr**, que será
usado na arguição. O Trokr segue o contrato dele: rotas, status HTTP, estados
da proposta, saldos, `application/problem+json` e tipos de log.

```bash
docker compose down -v
TROKR_DEMO_ENDPOINTS=true docker compose up --build -d   # Windows: $env:TROKR_DEMO_ENDPOINTS="true"; docker compose up --build -d
```

- **Blocos 0 a 6 e 9:** pelo Swagger, em http://localhost:8090/swagger-ui.html.
- **Bloco 7 (concorrência):** `./cenario-final.sh`, depois `./corrida.sh {N1}` e
  `./corrida.sh {N2} {N3}`. Os dois scripts são os do Apêndice A do roteiro e
  precisam do `jq` (`sudo apt install jq`). Para o 409 vir do `@Version`, suba
  com `TROKR_ATRASO_MS=2000` (no projeto de referência: `TROKR_DEMO_ATRASOMS`).
- **Bloco 8 (erros de protocolo):** `curl -i` como no roteiro.

**Fluxo da proposta**, como no roteiro: `POST /propostas` → `POST /{id}/enviar-homologacao`
→ `POST /{id}/aprovar` → `POST /{id}/contrapropostas` → `POST /{contra}/enviar`
→ `POST /{contra}/aceitar` → `POST /{raiz}/aceitar-contraproposta` → `PATCH /{raiz}/finalizar`.
O aceite tem duas etapas, e a contraproposta vencedora continua em
`NEGOCIADO_CONTRA` depois da finalização.

**Diferenças em relação ao projeto de referência** (o roteiro pede que fiquem
anotadas aqui):

| Ponto | Projeto de referência | Trokr |
|---|---|---|
| Campos específicos do item | dentro de `detalhes` | no próprio JSON (`pesoGramas`, `duracaoMinutos`, `nivel`), só o do tipo |
| Variável do atraso da Aula 10 | `TROKR_DEMO_ATRASOMS` | `TROKR_ATRASO_MS` |
| Ação errada na máquina da contraproposta | `PROPOSTA_TRANSICAO_INVALIDA` | `CONTRAPROPOSTA_TRANSICAO_INVALIDA` (409, mesmo significado) |
| Quem age (`?usuarioId=`) | não existe | opcional em aceitar, aceitar-contraproposta, recusar e avaliar; se informado e não for a pessoa certa, 403 |
| Códigos a mais | — | `AVALIACAO_AUTOR_INVALIDO` e `PROPOSTA_ACAO_NAO_AUTORIZADA` (403), `RESTRICAO_DE_INTEGRIDADE` (409), `ERRO_INTERNO` (500), `MIDIA_NAO_SUPORTADA` (415) |
| Log a mais | — | `FINALIZACAO_RECUSADA` (AFTER_ROLLBACK, Aula 10) |
| Campos a mais na proposta | — | `tipo`, `podeMudar`, `propostaPaiId` (igual a `propostaAnteriorId`) |
| Usuário | — | aceita também `cidade` (opcional) |

O registro de falhas do Apêndice C e o guia de estudo para a arguição estão em
[`ROTEIRO_DE_TESTES.md`](ROTEIRO_DE_TESTES.md).

## Entrega 2 — onde está cada padrão

| Padrão | Onde está | O que mostrar na arguição |
|---|---|---|
| **Domain Model rico** | `model/Proposta`, `model/Avaliacao`, `model/Usuario` | Não há `setStatus()`: o status só muda por métodos de negócio (`submeter`, `aceitar`, `finalizar`...). Regras como "só o dono da raiz aceita" e "só propostas Ativas recebem contraproposta" moram na entidade. |
| **State** (Opção A) | `model/state/proposta` (6 estados da raiz) e `model/state/contraproposta` (6 da contraproposta) | Duas interfaces independentes. Cada estado decide as próprias transições e lança `TransicaoInvalidaException` (HTTP 409) para as inválidas. O efeito cascata (`encerrarPorFimDaRaiz`) também é decidido por cada estado. |
| **Observer** | `event/TrocaConcluidaEvent` + `listener/*` (5 listeners) | O `PropostaService` só publica o evento, e somente quando a troca chega ao estado final. Os listeners não se conhecem: incluir um sexto não mexe em nada que já existe. |
| **Adapter** | `adapter/RegistroDeEventos` (porta) + `adapter/mongo/RegistroDeEventosMongo` (adaptador) | O `LogEventoService` depende só da interface. O documento Mongo (`LogEvento`) nunca sai do pacote do adaptador. |
| **Strategy** | `strategy/EstrategiaCredito` + `CreditoProduto`, `CreditoServico`, `CreditoExperiencia`; contexto em `service/CalculadoraCreditoService` | Um `Map<CategoriaItem, EstrategiaCredito>` montado pelo Spring, sem nenhum switch por categoria. Cada estratégia tem teste unitário próprio, sem Spring. |
| **Facade** | `service/PropostaService` | O controller só conhece o `PropostaService`. `finalizarNegociacao()` coordena o State, a persistência e o evento. O Javadoc da classe explica a ordem e o que ela não faz. |

Créditos por categoria (constante `CREDITOS` em cada estratégia): PRODUTO = 10,
SERVICO = 15, EXPERIENCIA = 20.

Diagramas: `diagramas/DiagramaDePadroesEntrega2.png` (visão da Entrega 2),
`DiagramaDeClasseV2.png` (completo) e os dois diagramas de estado V2. Os
fontes PlantUML são os `.txt` da mesma pasta.

Sequência: `DiagramaDeSequenciaV1.png` é o ciclo da proposta (Entrega 1);
`DiagramaDeSequenciaFinalizacaoV2.png` (fonte Mermaid no `.mmd`) é a finalização
como o código faz hoje (Facade, State, Observer, `@Version`, AFTER_COMMIT,
retry e disjuntor).

### Roteiro de teste (Aula 9)

Importe `COLLECTION_INSOMNIA.json` e rode a pasta **"0. Roteiro Aula 9"** em
ordem, com o banco zerado (`docker compose down -v` e depois
`docker compose up --build`). Os passos marcados "ERRO esperado" precisam
falhar. Ao fim, `GET /usuarios` mostra A = 10 e B = 15 créditos, e existem
2 avaliações pendentes, 1 histórico e 1 log `INFO_GERAL` (troca concluída).

### Testes unitários

`./mvnw test -Dtest='PropostaStateTest,AvaliacaoTest,Credito*Test,CalculadoraCreditoServiceTest,FabricasDeItemTest,CatalogoDeFabricasTest,NotificarUsuariosListenerTest,Notificacao*Test,PilhaDeNotificacaoTest,AtrasoDemoTest'`
roda os testes de domínio, State, Strategy, das fábricas de item, das camadas de notificação (retry e disjuntor) da notificação e do
atraso de demonstração sem precisar de banco. O
`TrokrApplicationTests` continua exigindo Postgres e Mongo no ar.

## Entrega Final — Aula 10: concorrência

Três condições de corrida, uma ferramenta para cada.

| Corrida | O que acontecia | Causa | Ferramenta | Onde |
|---|---|---|---|---|
| 1 · a mesma troca finalizada duas vezes (`./corrida.sh <raiz>`) | 200 e 200, duas avaliações por lado, crédito em dobro | check-then-act: as duas requisições conferiram o estado "Negociado" antes de qualquer uma gravar | **Lock otimista**: `@Version` na Proposta. A segunda grava com versão velha, afeta 0 linhas, a transação inteira é desfeita e o cliente recebe **409** `PROPOSTA_ALTERADA_CONCORRENTEMENTE` | `Proposta.versao`, `GlobalExceptionHandler.handleConcorrencia` |
| 2 · duas trocas do mesmo usuário (`./corrida.sh <raiz1> <raiz2>`) | 200 e 200, mas o saldo de A perdia uma das somas (30 + 10 + 15 = 45) | read-modify-write: o listener lia o saldo, somava no Java e gravava | **UPDATE atômico**: `adicionarCreditos` soma no próprio banco; o segundo UPDATE espera o lock da linha e soma sobre o valor já commitado | `UsuarioRepository.adicionarCreditos`, `AtribuirCreditosListener` |
| 3 · o efeito fantasma | Com a Corrida 1 resolvida, o Postgres desfazia a segunda finalização, mas o log no Mongo e a notificação já tinham saído | o rollback só alcança a transação do Postgres; Mongo e mensagens ficam fora dela | **Evento depois do commit**: `@TransactionalEventListener(AFTER_COMMIT)` no log e na notificação | `RegistrarLogEventoListener`, `NotificarUsuariosListener` |

Por que cada ferramenta:

- **Corrida 1, lock otimista e não pessimista.** A regra "só finaliza se o
  estado permitir" mora no State, não cabe num WHERE. O conflito é raro, e
  quem perde deve ser avisado (a troca já acabou), não esperar numa fila. O
  pessimista travaria toda finalização e arriscaria deadlock com a cascata.
  Não há retry automático: repetir leria a Proposta já Finalizada e o State
  recusaria do mesmo jeito. De brinde, o `@Version` também protege aceitar
  duas contrapropostas em abas diferentes e a cascata x o autor enviando.
- **Corrida 2, UPDATE atômico e não `@Version` no Usuario.** "Somar ao
  saldo" é aritmética pura: cabe num SQL. Com `@Version`, uma troca correta
  receberia 409 por causa de OUTRA troca. Aqui as duas estão certas e as duas
  dão 200. Cuidados tomados: depois do UPDATE, ninguém salva o mesmo Usuario
  na transação (o objeto Java ficou com o saldo antigo); e os dois créditos
  são aplicados sempre em ordem crescente de id, para que as trocas A↔B e
  B↔A ao mesmo tempo não travem uma à outra (deadlock).
- **Corrida 3, o que fica dentro e o que espera.** Avaliação, Histórico e
  Créditos gravam no Postgres e continuam DENTRO da transação: se ela for
  desfeita, eles somem junto. Log (Mongo) e notificação (fora do sistema)
  esperam o commit. Bônus: `RegistrarFinalizacaoRecusadaListener`
  (`AFTER_ROLLBACK`) registra `FINALIZACAO_RECUSADA` no LogEvento quando uma
  finalização é desfeita.

O atraso de demonstração fica em `config/AtrasoDemo` (modelo da aula), lido
de `trokr.demo.atraso-ms`; é aplicado em `finalizarNegociacao()`, entre o
estado conferir e a Proposta ser gravada.

**Como reproduzir** (no Windows, os scripts rodam no Git Bash):

1. Suba com o atraso ligado: `TROKR_ATRASO_MS=2000 docker compose up --build`
   (no PowerShell: `$env:TROKR_ATRASO_MS=2000; docker compose up --build`).
2. `./preparar-cenario.sh` cria três usuários e duas negociações em Negociado
   com o usuário A em comum, e imprime os comandos prontos.
3. `./corrida.sh <raiz1>` deve dar **200 e 409**, com duas avaliações, um
   histórico, crédito uma vez só, um `INFO_GERAL` em `GET /logs` e duas
   notificações.
4. Rode `./preparar-cenario.sh` de novo e então `./corrida.sh <raiz1> <raiz2>`:
   deve dar **200 e 200**, com o saldo de A igual a 20 (10 + 10).
5. Entregue com o atraso em 0 (é o padrão).

Limite conhecido: o `@Version` protege o intervalo dentro de uma requisição.
Se A abre a tela, pensa cinco minutos e B altera a proposta nesse meio-tempo,
a versão teria de ir até o cliente e voltar (ETag/If-Match), o que está fora
do escopo da disciplina.

## Entrega Final — Aula 11: contrato de erro

Toda resposta de erro é um **ProblemDetail** (RFC 9457,
`application/problem+json`), atendendo três leitores: o código do cliente
(`status` + `codigo` estável), a pessoa na tela (`title` fixo + `detail`
desta ocorrência) e quem opera (`instance` e, no 500, `ocorrencia`).

```json
{
  "type": "https://trokr.local/erros/proposta-transicao-invalida",
  "title": "Ação não permitida no estado atual",
  "status": 409,
  "detail": "Transição inválida: Rascunho → Ativa",
  "instance": "/propostas/1/aprovar",
  "codigo": "PROPOSTA_TRANSICAO_INVALIDA",
  "timestamp": "2026-10-27T10:42:10Z"
}
```

### Hierarquia de exceções

Exceções de domínio (a regra disse "não") herdam de `TrokrException`, que é
`sealed`, unchecked e carrega o `codigo`. Elas não conhecem HTTP (Opção B): o
`GlobalExceptionHandler` faz o mapeamento, porque na Aula 12 as mesmas
exceções aparecem num agendamento, sem requisição nenhuma. Falhas técnicas
(NullPointerException, Mongo fora do ar, notificação indisponível) ficam fora
da hierarquia.

| Exceção | Status | Quando |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | id inexistente |
| `TransicaoInvalidaException` | 409 | o estado atual não permite a ação |
| `RegraNegocioException` | 422 | dado bem formado que viola uma regra |
| `AcessoNegadoException` | 403 | alguém agindo em nome de outra pessoa |

`AcessoNegadoException` é uma quarta subclasse, além das três mínimas do
slide: a reação esperada do cliente é diferente (não há dado para corrigir,
a pessoa simplesmente não pode fazer aquilo).

Os estados não lançam mais `IllegalStateException`. As que sobraram no
projeto (fábrica de estados, guardas internas da Proposta, estratégia de
crédito ausente, crédito não aplicado) são **defeitos** de propósito e viram 500.

### Política do handler

| Exceção | Status | `codigo` | Registro |
|---|---|---|---|
| `RecursoNaoEncontradoException` | 404 | específico | nenhum |
| `TransicaoInvalidaException` | 409 | específico | nenhum |
| `RegraNegocioException` | 422 | específico | nenhum |
| `AcessoNegadoException` | 403 | específico | nenhum |
| `OptimisticLockingFailureException` | 409 | `PROPOSTA_ALTERADA_CONCORRENTEMENTE` | `log.warn` |
| `DataIntegrityViolationException` | 409 | `RESTRICAO_DE_INTEGRIDADE` | `log.warn` |
| Validação (`@Valid`) | 400 | `VALIDACAO` + lista `campos` | nenhum |
| Erros do Spring MVC (JSON quebrado, id não numérico, método) | 400 / 405 | gerado pelo Spring | nenhum |
| Qualquer outra `Exception` | 500 | `ERRO_INTERNO` + `ocorrencia` | `log.error` + LogEvento `ERRO_SISTEMA` |

Só defeito vai para o LogEvento. O 500 nunca devolve `ex.getMessage()`; o id
em `ocorrencia` é o mesmo gravado em `GET /logs?tipo=ERRO_SISTEMA`. O
handler herda de `ResponseEntityExceptionHandler`, que já responde as
exceções do Spring MVC como ProblemDetail.

### Catálogo de códigos

| Código | Status | Significado |
|---|---|---|
| `USUARIO_NAO_ENCONTRADO` | 404 | usuário inexistente (por id ou e-mail) |
| `ITEM_NAO_ENCONTRADO` | 404 | item inexistente |
| `PROPOSTA_NAO_ENCONTRADA` | 404 | proposta inexistente |
| `AVALIACAO_NAO_ENCONTRADA` | 404 | avaliação inexistente |
| `PROPOSTA_TRANSICAO_INVALIDA` | 409 | a máquina da raiz não permite a ação, ou ação da máquina errada (ex.: aprovar uma contraproposta) |
| `CONTRAPROPOSTA_TRANSICAO_INVALIDA` | 409 | a máquina da contraproposta não permite a ação |
| `PROPOSTA_NAO_RECEBE_CONTRAPROPOSTA` | 409 | contraproposta para uma proposta que não está Ativa |
| `PROPOSTA_ALTERADA_CONCORRENTEMENTE` | 409 | perdeu a corrida para outra requisição (`@Version`) |
| `RESTRICAO_DE_INTEGRIDADE` | 409 | restrição do banco (ex.: remover item em uso) |
| `ITEM_DE_OUTRO_USUARIO` | 422 | oferecer item que não é seu |
| `CONTRAPROPOSTA_NA_PROPRIA_PROPOSTA` | 422 | contrapropor a própria proposta |
| `CONTRAPROPOSTA_DE_CONTRAPROPOSTA` | 422 | contrapropor uma contraproposta |
| `JUSTIFICATIVA_OBRIGATORIA` | 422 | cancelar contraproposta sem justificativa |
| `AVALIACAO_JA_REALIZADA` | 422 | avaliar de novo |
| `AVALIACAO_NOTA_INVALIDA` | 422 | nota fora de 1 a 5 |
| `EMAIL_JA_CADASTRADO` | 422 | e-mail já usado por outro usuário |
| `ITEM_DADOS_INCOMPLETOS` | 422 | item sem o campo do seu tipo (Aula 12) |
| `ITEM_CAMPO_INCOMPATIVEL` | 422 | item com campo de outro tipo (Aula 12) |
| `ITEM_CATEGORIA_IMUTAVEL` | 422 | mudar a categoria de um item no `PUT` (Aula 12) |
| `PROPOSTA_ACAO_NAO_AUTORIZADA` | 403 | aceitar, recusar ou desistir sem ser parte |
| `AVALIACAO_AUTOR_INVALIDO` | 403 | preencher a avaliação de outra pessoa |
| `VALIDACAO` | 400 | campos inválidos (lista em `campos`) |
| `REQUISICAO_INVALIDA` | 400 | JSON malformado, enum inexistente, tipo errado na URL |
| `ROTA_NAO_ENCONTRADA` | 404 | caminho que não existe na API |
| `METODO_NAO_SUPORTADO` | 405 | método HTTP não aceito no caminho |
| `ERRO_INTERNO` | 500 | defeito; informe a `ocorrencia` ao suporte |

### Notificação com falha e retry

Na Aula 11, o serviço de notificação simulado (hoje `NotificacaoSimuladaAdapter`)
passou a demorar `trokr.notificacao.latencia-ms` e a falhar com probabilidade
`trokr.notificacao.taxa-falha`, lançando `NotificacaoIndisponivelException`
(exceção técnica, fora da hierarquia). O retry com espera crescente (200, 400,
800 ms), só para essa exceção, morava no `NotificarUsuariosListener`. Com o
serviço morto, cada finalização gastava ~2 s e 6 chamadas inúteis: o ponto de
partida da [Aula 13](#entrega-final--aula-13-tolerância-a-falhas-hexagonal-e-decorator),
onde o retry virou camada e ganhou um disjuntor.

## Entrega Final — Aula 12: Factory e Builder

Os tipos de oferta passaram a ter dados próprios: produto tem **peso**,
serviço tem **duração** e experiência tem **nível**. Cada tipo virou uma
subclasse de `Item`, e quem cria cada uma é a sua fábrica.

### Herança: SINGLE_TABLE com a categoria como discriminador

| Classe | Campo próprio | Valor da coluna `categoria` |
|---|---|---|
| `model/Item` (abstrata) | `titulo`, `descricao`, `usuarioProprietario`, `dataCriacao` | — |
| `model/ItemProduto` | `pesoGramas: Integer` | `PRODUTO` |
| `model/ItemServico` | `duracaoMinutos: Integer` | `SERVICO` |
| `model/ItemExperiencia` | `nivel: NivelExperiencia` (`INICIANTE`, `INTERMEDIARIO`, `AVANCADO`) | `EXPERIENCIA` |

- Uma tabela só (`item`). A coluna `categoria`, criada na Aula 9, virou o
  `@DiscriminatorColumn`: os itens antigos carregam como a subclasse certa.
- `getCategoria()` é **abstrato**: cada subclasse responde com a sua
  constante. A Strategy de créditos (Aula 9) continua chamando
  `item.getCategoria()` e não mudou uma linha; os testes `Credito*Test`
  passam como antes.
- **Sem `instanceof`**: com `LAZY` o Hibernate entrega um proxy de `Item`, e o
  `instanceof ItemProduto` falharia. Quem precisa do tipo pergunta ao objeto:
  `getCategoria()` e `dadosDoTipo()` são polimórficos (é o `dadosDoTipo()` que
  o `ItemResponseDTO` usa).
- Preço do SINGLE_TABLE: `peso_gramas`, `duracao_minutos` e `nivel` aceitam
  nulo no banco. A garantia "todo produto tem peso" passa para o código, na
  criação: é o trabalho da fábrica.

### Factory Method: uma fábrica por tipo e um catálogo que escolhe

| Peça | Onde está | Papel |
|---|---|---|
| `FabricaDeItem` | `factory/FabricaDeItem` | `categoria()`, `validar(DadosDoTipo)`, `criar(ItemRequestDTO, Usuario)` |
| 3 fábricas | `factory/FabricaItemProduto`, `FabricaItemServico`, `FabricaItemExperiencia` | Exigem o campo do tipo, rejeitam os dos outros e montam com o builder |
| Catálogo | `factory/CatalogoDeFabricas` | `Map<CategoriaItem, FabricaDeItem>` montado da lista injetada pelo Spring, igual ao da `CalculadoraCreditoService` |

Fluxo de `POST /itens`: o `ItemService` recebe o DTO e busca o dono → o
catálogo escolhe a fábrica pela categoria → a fábrica valida e lança as
exceções da Aula 11 → o builder monta e o service salva, **sem switch e sem
`new` de subclasse**.

| Regra de criação | Exemplo | Resposta |
|---|---|---|
| Campo obrigatório do tipo | produto sem `pesoGramas` | 422 `ITEM_DADOS_INCOMPLETOS` |
| Campo de outro tipo | serviço com `pesoGramas` | 422 `ITEM_CAMPO_INCOMPATIVEL` |
| Valor sem sentido | `pesoGramas` ≤ 0 | 400 `VALIDACAO` (Bean Validation) |
| Trocar a categoria no `PUT` | produto virar serviço | 422 `ITEM_CATEGORIA_IMUTAVEL` |

**Teste de fogo: um tipo novo (ITEM DIGITAL, `tamanhoMb`)**. Só se
acrescenta `ItemDigital`, `FabricaItemDigital`, `CreditoDigital` e os testes;
só se modificam `CategoriaItem` (ganha `DIGITAL`) e o DTO (ganha
`tamanhoMb`). `ItemService`, `CatalogoDeFabricas`,
`CalculadoraCreditoService` e `Proposta` não mudam.

### Builder: `@SuperBuilder` na hierarquia

`@Builder` não enxerga os campos da classe mãe; `@SuperBuilder`, em `Item` e
em cada subclasse, enxerga:

```java
ItemProduto bicicleta = ItemProduto.builder()
        .titulo("Bicicleta aro 29")       // campo de Item
        .descricao("Pouco usada")
        .usuarioProprietario(ana)
        .pesoGramas(14500)                 // campo de ItemProduto
        .build();
```

Cuidados de entidade JPA: `@NoArgsConstructor(access = PROTECTED)` (o JPA
precisa do construtor vazio), nada de `@Data` (só `@Getter`), e `dataCriacao`
vem do `@CreationTimestamp`, não de um inicializador. O builder não valida
nada: quem valida é a fábrica, antes do `build()`. Nos testes, o builder
monta itens sem fábrica (`Fixtures.item`).

Diagramas: `diagramas/DiagramaFactoryBuilderAula12.png` (visão focada) e
`DiagramaDeClasseV2.png` (completo, com as classes novas marcadas `<<Aula 12>>`).

### Como demonstrar

Depois do roteiro da Aula 9, rode a pasta **"Aula 12 · Factory e Builder"** da
coleção: dois itens criados (201), as três recusas 422 com os códigos acima e
o 400 de peso negativo. `GET /itens` mostra cada item só com o campo do seu
tipo (um produto não aparece com `"nivel": null`).

> A tabela `item` ganhou colunas. Na primeira subida depois desta versão,
> zere o banco: `docker compose down -v` e `docker compose up --build`.

## Entrega Final — Aula 13: tolerância a falhas, Hexagonal e Decorator

Notificação é dependência **acessória**: a troca vale sem ela. Objetivo:
quando o serviço de notificação cair, as finalizações continuam rápidas, as
trocas são concluídas, e cada notificação que não saiu fica registrada.

| Classe (`notificacao/`) | Papel |
|---|---|
| `NotificacaoPort` | A **porta** (Hexagonal): `notificar(usuarioId, mensagem)`. A única coisa que o listener conhece |
| `NotificacaoSimuladaAdapter` | O **adaptador**: o `NotificacaoService` da Aula 11, renomeado. Corpo igual, mais o interruptor na primeira linha |
| `SimulacaoNotificacao` | O interruptor: serviço no ar ou fora do ar (`AtomicBoolean`) |
| `NotificacaoComRetry` | **Decorator** 1: o laço de retry que estava no listener |
| `NotificacaoComDisjuntor` | **Decorator** 2: o circuit breaker (fechado → aberto → meio-aberto) |
| `NotificacaoConfig` | Monta a pilha como `@Bean @Primary`: retry por fora, disjuntor por dentro, adaptador no centro |
| `NotificacaoProperties` | As chaves `trokr.notificacao.*` num record (`@ConfigurationProperties`) |
| `controller/DemoNotificacaoController` | `PATCH /demo/notificacao/fora-do-ar` e `/no-ar`, só com `trokr.demo.endpoints: true` |

```
NotificarUsuariosListener ─▶ NotificacaoComRetry ─▶ NotificacaoComDisjuntor ─▶ NotificacaoSimuladaAdapter
          (só a regra)          (tenta de novo)        (para de tentar)            (fala com o "serviço")
```

O listener ficou só com a regra: para quem mandar, o que mandar, e o que
fazer se a pilha desistir (`log.error` + `ERRO_SISTEMA`, sem deixar nada subir).

**Disjuntor** (`limite-falhas: 5`, `tempo-aberto: 30s`): fechado, conta as
falhas seguidas; na 5ª, abre. Aberto, nada passa: registra
`NOTIFICACAO_PENDENTE` no log e **retorna normalmente**, então o retry para
na hora. Passados 30 s, deixa uma chamada de teste (meio-aberto): funcionou,
fecha; falhou, abre de novo.

**Por que o disjuntor é um Decorator?** Implementa a mesma `NotificacaoPort`
que envolve: o listener não sabe que ele existe, e ele entra ou sai da pilha
mudando só a `NotificacaoConfig`. **Por que por dentro do retry?** Assim cada
tentativa do retry passa por ele e é contada, e quando ele abre o retry para
na primeira tentativa. Por fora, ele só veria a falha final de cada pessoa,
depois de todas as esperas.

Diagramas: `diagramas/DiagramaNotificacaoAula13.png` (visão focada) e
`DiagramaDeClasseV2.png` (completo, classes novas marcadas `<<Aula 13>>`).

### Como demonstrar (passo 8 da aula)

1. `./preparar-cenario.sh`: ele deixa duas negociações em NEGOCIADO_RAIZ (Raiz 1 e Raiz 2). O roteiro do professor faz o mesmo com o `cenario-final.sh` (N4 a N7).
2. `curl -X PATCH localhost:8090/demo/notificacao/fora-do-ar` (ou pelo Swagger, grupo "Demonstração").
3. `PATCH /propostas/{Raiz 1}/finalizar`: pessoa A, tentativas 1, 2 e 3 falham → falha definitiva;
   pessoa B, tentativas 4 e 5 falham → **"Disjuntor ABERTO"**, a 3ª vira pendente.
4. `PATCH /propostas/{Raiz 2}/finalizar`: responde rápido, nenhuma chamada ao serviço, as duas pendentes na hora.
5. `GET /logs?tipo=NOTIFICACAO_PENDENTE` mostra **três** pendências.
6. `PATCH /demo/notificacao/no-ar`, espere 30 s, rode o `preparar-cenario.sh` de novo e finalize
   uma troca: **"Disjuntor FECHADO"** no log e as notificações saem.

O `PilhaDeNotificacaoTest` reproduz exatamente os passos 3 a 5, e o
`NotificacaoComDisjuntorTest` os três estados com um relógio de teste.

### Proxy (sem código)

Mesma forma do Decorator, outra intenção: controla o acesso, e quem monta é
a infraestrutura. O Trokr usa desde a Aula 3: os repositories (interfaces
sem implementação), o `@Transactional` do `PropostaService` e o lazy loading
do Hibernate (a armadilha do `instanceof` na Aula 12). Armadilha: o proxy só
existe entre dois beans, então `this.metodoTransacional()` fala direto com o
objeto real e a anotação vira enfeite; e um objeto criado com `new` nunca
ganha proxy. Por isso as camadas da notificação, montadas com `new` na
`NotificacaoConfig`, não dependem de nenhuma anotação do Spring.

## Entrega Final — Aula 14: documentação viva e ADRs

### Swagger / OpenAPI

- Tela: **http://localhost:8090/swagger-ui.html** · contrato: `/v3/api-docs` (e `/v3/api-docs.yaml`).
- `pom.xml`: `springdoc-openapi-starter-webmvc-ui` **3.2.0** (a linha do Spring Boot 4.1).
- `application.yml`: rotas, ordem alfabética, "Try it out" já ativo e o tempo de cada requisição.
- `config/OpenApiConfig`: título "Trokr API 1.0.0", descrição e o servidor `localhost:8090`.
- `@Tag` em todos os controllers (Usuários, Itens, Propostas, Avaliações,
  Histórico, Logs, Demonstração) e `@Hidden` no `HelloController`.
- `@Operation` em todas as operações, `@Parameter` com exemplo nos ids, e
  `@Schema` com descrição e exemplo nos DTOs de entrada.
- **Contrato de erro visível**: cada operação declara com `@ApiResponse` só
  os códigos que pode devolver (400 / 403 / 404 / 409 / 422), com
  `application/problem+json` e o esquema `ProblemDetail`.
  `springdoc.override-with-generic-response: false` evita que o springdoc
  acrescente os erros do `GlobalExceptionHandler` em todas.
- O grupo "Demonstração" só aparece com `trokr.demo.endpoints: true`.
  `application-prod.yml` (perfil `prod`) desliga Swagger, contrato e
  endpoints de demonstração.

**Uma troca inteira pelo navegador**: no grupo Propostas, na ordem do
roteiro do professor (criar, `/enviar-homologacao`, `/aprovar`, contrapropor,
`/enviar`, `/aceitar`, `/aceitar-contraproposta`, `/finalizar`). Finalizar de
novo responde 409 com `application/problem+json`.

### ADRs

Em [`docs/adr/`](docs/adr/), no modelo da aula (Status, Contexto,
Alternativas consideradas, Decisão, Consequências):

| ADR | Decisão |
|---|---|
| [0001](docs/adr/0001-postgres-para-o-negocio-mongodb-para-o-log.md) | Postgres para o negócio, MongoDB para o log de eventos |
| [0002](docs/adr/0002-bloqueio-otimista-com-version.md) | Bloqueio otimista com `@Version` na Proposta |
| [0003](docs/adr/0003-disjuntor-escrito-a-mao.md) | Disjuntor e retry escritos à mão, como Decorators, em vez do Resilience4j |
| [0004](docs/adr/0004-heranca-de-item-em-single-table.md) | Herança de Item em SINGLE_TABLE |
| [0005](docs/adr/0005-monolito-modular.md) | Monolito modular em vez de microsserviços |

Guia de testes (complemento do roteiro do professor), registro de falhas e perguntas prováveis: [`ROTEIRO_DE_TESTES.md`](ROTEIRO_DE_TESTES.md).

## Stack e decisões da Aula 1

- **Java 21** + **Spring Boot 4.1.0**
- **Maven** (com Maven Wrapper, `mvnw`/`mvnw.cmd`) em vez de Gradle — escolhido
  por ter XML mais verboso porém mais fácil de ler linha a linha numa aula
  expositiva do que o DSL Groovy/Kotlin do Gradle, e por ser o build tool que
  a maioria dos tutoriais e da documentação oficial do Spring usa como
  exemplo padrão.
- **Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Boot Actuator**
- **Bean Validation** (`spring-boot-starter-validation`) para validar os DTOs
  de entrada
- **Lombok** (opcional): usado só para reduzir boilerplate de getters/setters/
  construtores nas entidades (`@Getter`, `@Setter`, `@NoArgsConstructor`,
  `@AllArgsConstructor`). Não é foco da disciplina — se preferir, pode remover
  a dependência e escrever os métodos manualmente sem mudar nada da
  arquitetura.
- **Spring Boot DevTools** (opcional, escopo `runtime`): reinicia a aplicação
  sozinho quando as classes são recompiladas, agilizando o ciclo de
  desenvolvimento (ver ["Ciclo de desenvolvimento"](#ciclo-de-desenvolvimento)).
  Com `optional=true` no `pom.xml`, não entra no `.jar` final gerado pelo
  Docker, então não tem efeito em produção.
- **PostgreSQL** via Docker Compose

## Como subir o ambiente

Pré-requisito: Docker e Docker Compose instalados. Não é necessário ter Java
ou Maven instalados na máquina — tudo é compilado dentro do container.

```bash
docker compose up
```

Isso sobe três containers:

- `trokr-db`: PostgreSQL 16, com um volume nomeado para persistir os dados
  entre reinicializações
- `trokr-mongo`: MongoDB 8 (Aula 8), onde fica o `LogEvento`
- `trokr-app`: a aplicação Spring Boot, compilada a partir do código-fonte
  via Dockerfile multi-stage, exposta na porta `8090` do host

Para derrubar o ambiente:

```bash
docker compose down
```

Para derrubar o ambiente **e apagar os dados do banco**:

```bash
docker compose down -v
```

## Ciclo de desenvolvimento

Como o código Java é compilado dentro do container, uma mudança no fonte só
aparece depois que a imagem é reconstruída. Para não ter que ficar rodando
`docker compose down` + `docker compose up --build` a cada alteração, use uma
das opções abaixo.

### Opção A — `docker compose watch` (recomendada, sem instalar Java)

```bash
docker compose watch
```

Não precisa rodar `docker compose up` antes: o `watch` já sobe `db` + `app` e
só então começa a observar `src/` e o `pom.xml` (bloco `develop.watch` no
`docker-compose.yml`). Ao salvar um arquivo, ele reconstrói a imagem do `app`
e recria o container automaticamente. O cache de camadas do Docker é
aproveitado — as dependências Maven já baixadas **não** são rebaixadas, então
sobra basicamente o `mvn package` (alguns segundos a algumas dezenas de
segundos). Requer Docker Compose 2.22 ou mais novo.

É só salvar o arquivo e esperar o container voltar.

> **Opcional — `docker compose up --watch`.** Faz o mesmo rebuild automático,
> mas com o comportamento do `up` de sempre: mostra os logs de **todos** os
> serviços (`db` e `app`, não só o `app`) e o `Ctrl+C` **derruba** os
> containers. Já o `docker compose watch` "cru" mostra só os logs dos serviços
> observados e, ao sair com `Ctrl+C`, **deixa os containers rodando** — nesse
> caso é preciso `docker compose down` depois. Use o que preferir; o resultado
> do hot reload é idêntico.

### Opção B — rodar a aplicação localmente com DevTools (mais rápido)

Se você tem Java 21 instalado (ou usa a IDE para rodar), suba só o banco no
Docker e a aplicação na máquina:

```bash
docker compose up db mongo -d
./mvnw spring-boot:run
```

Com o **Spring Boot DevTools** no classpath, a aplicação reinicia sozinha
(1–2s) sempre que as classes são recompiladas — o `mvnw spring-boot:run`
recompila ao detectar mudança, e no IntelliJ basta acionar "Build Project"
(Ctrl+F9 / Cmd+F9).

Nesse caso, as variáveis de ambiente de conexão com o banco (`DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) já têm valores padrão em
`application.yml` compatíveis com o `docker-compose.yml` rodando em
`localhost`.

## Estrutura de pastas

O projeto segue uma arquitetura em camadas simples — cada pacote tem uma
responsabilidade e conversa só com o pacote "abaixo" dele:

```
src/main/java/com/trokr/
├── controller/   # Recebe requisições HTTP, valida entrada, devolve DTOs
├── service/      # Regras de negócio e orquestração das operações
├── repository/   # Acesso a dados via Spring Data JPA (sem lógica própria)
├── model/        # Entidades JPA (representam as tabelas do banco)
├── dto/          # Objetos de entrada/saída da API (não expõem as entidades)
├── notificacao/  # Aula 13: porta, adaptador e as camadas de retry e disjuntor
├── factory/      # Aula 12: FabricaDeItem, uma fábrica por categoria e o catálogo
└── exception/    # Exceções da aplicação e tratamento delas
```

O fluxo de uma requisição é sempre:
`Controller → Service → Repository → Banco de dados`, e a resposta volta
pelo mesmo caminho, sendo convertida de entidade para DTO no Controller.

Decisões desta fase que valem explicar:

- **Sem interfaces para os Services** (`UsuarioService`, `ItemService` são
  classes concretas): como só existe uma implementação de cada, uma
  interface por cima só adicionaria indireção sem benefício real agora.
- **Repositories são só `extends JpaRepository`**, sem nenhuma abstração
  genérica por cima — o Spring Data JPA já resolve isso.
- **DTOs são `record`s do Java**, convertidos manualmente de/para entidade
  (métodos `fromEntity` nos DTOs de resposta). Sem MapStruct ou biblioteca
  de mapeamento.
- **`Item` era uma entidade genérica** nesta fase. Desde a Aula 12 é
  abstrato, com `ItemProduto`, `ItemServico` e `ItemExperiencia` (ver
  [Aula 12](#entrega-final--aula-12-factory-e-builder)).
- **Relação `Item → Usuario` é unidirecional**: o item conhece seu dono, mas
  `Usuario` não guarda uma lista de itens. Evita decisões de cascade/fetch
  type que ainda não fazem sentido discutir nesta fase.
- **Tratamento de erro é só um `@ExceptionHandler` genérico** para "não
  encontrado", devolvendo 404. Erros de validação (`@Valid`) já viram 400
  automaticamente pelo Spring, sem código extra. Um tratamento de erros
  robusto e padronizado é conteúdo de uma aula futura.
- **`ddl-auto: update`** no Hibernate cria/ajusta as tabelas automaticamente
  a partir das entidades — é uma simplificação temporária para o início do
  curso, documentada como tal no `application.yml`. Será substituído por
  migrations versionadas (Flyway/Liquibase) quando controle de schema passar
  a importar.

## Endpoints disponíveis

Base URL: `http://localhost:8090` pelo Docker Compose (porta 8090 do host → 8080 do container) ou `http://localhost:8080` rodando com `./mvnw spring-boot:run`.

### Hello World

Endpoint mais simples possível, sem tocar em banco de dados — bom primeiro
teste no Postman/Insomnia para confirmar que a aplicação está no ar.

```
GET /hello
```

```
Hello, Trokr!
```

### Health check

```
GET /actuator/health
```

```json
{ "status": "UP" }
```

### Usuarios

| Método | Rota             | Descrição               |
|--------|------------------|--------------------------|
| GET    | `/usuarios`      | Lista todos os usuários |
| GET    | `/usuarios/{id}` | Busca um usuário por id |
| POST   | `/usuarios`      | Cria um usuário         |
| PUT    | `/usuarios/{id}` | Atualiza um usuário     |
| DELETE | `/usuarios/{id}` | Remove um usuário       |

**Request — `POST /usuarios`**

```json
{
  "nome": "Maria Silva",
  "email": "maria@example.com"
}
```

**Response — `201 Created`**

```json
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "dataCriacao": "2026-08-02T21:18:54.056766"
}
```

### Itens

| Método | Rota          | Descrição            |
|--------|---------------|------------------------|
| GET    | `/itens`      | Lista todos os itens |
| GET    | `/itens/{id}` | Busca um item por id |
| POST   | `/itens`      | Cria um item          |
| PUT    | `/itens/{id}` | Atualiza um item      |
| DELETE | `/itens/{id}` | Remove um item        |

**Request — `POST /itens`**

```json
{
  "titulo": "Aula de violão",
  "descricao": "Aula básica de 1 hora, para iniciantes",
  "categoria": "SERVICO",
  "duracaoMinutos": 60,
  "usuarioId": 1
}
```

**Response — `201 Created`**

```json
{
  "id": 1,
  "titulo": "Aula de violão",
  "descricao": "Aula básica de 1 hora, para iniciantes",
  "categoria": "SERVICO",
  "usuarioId": 1,
  "usuarioNome": "Maria Silva",
  "dataCriacao": "2026-08-02T21:19:06.836283",
  "duracaoMinutos": 60
}
```

Cada categoria exige o seu campo: `pesoGramas` (PRODUTO), `duracaoMinutos`
(SERVICO) ou `nivel` (EXPERIENCIA: `INICIANTE`, `INTERMEDIARIO`, `AVANCADO`).

**Erros comuns**: `400` (dados inválidos, com a lista `campos`), `404`
(`USUARIO_NAO_ENCONTRADO`, `ITEM_NAO_ENCONTRADO`) e `422`
(`ITEM_DADOS_INCOMPLETOS`, `ITEM_CAMPO_INCOMPATIVEL`, `ITEM_CATEGORIA_IMUTAVEL`), sempre no formato
ProblemDetail descrito em [Contrato de erro](#entrega-final--aula-11-contrato-de-erro).

### Propostas e contrapropostas (Entrega 2)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/propostas` (`?apenasRaizes=true`) | Lista propostas |
| GET | `/propostas/{id}` | Busca uma proposta |
| GET | `/propostas/{id}/contrapropostas` | Contrapropostas concorrentes de uma raiz |
| POST | `/propostas` | Cria proposta raiz `{usuarioId, itemId}` (+ `titulo`, `descricao` opcionais) |
| POST | `/propostas/{id}/contrapropostas` | Contrapropõe `{usuarioId, itemId}` (+ `descricao` opcional) |
| POST | `/propostas/{id}/enviar-homologacao` | Raiz: RASCUNHO → HOMOLOGACAO |
| POST | `/propostas/{id}/recusar-homologacao` | Raiz: HOMOLOGACAO → RASCUNHO |
| POST | `/propostas/{id}/aprovar` | Raiz: HOMOLOGACAO → ATIVA |
| POST | `/propostas/{id}/editar` | Volta para RASCUNHO |
| POST | `/propostas/{id}/enviar` | Contra: RASCUNHO → EM_ANALISE |
| POST | `/propostas/{id}/aceitar` (`?usuarioId=` opcional) | Contra: EM_ANALISE → NEGOCIADO_CONTRA (1ª etapa) |
| POST | `/propostas/{id}/aceitar-contraproposta` (`?usuarioId=` opcional) | Raiz: ATIVA → NEGOCIADO_RAIZ (2ª etapa) |
| POST | `/propostas/{id}/recusar` (`?usuarioId=` opcional) | Contra em análise → RECUSADO |
| POST | `/propostas/{id}/desistir?usuarioId=` | Uma das partes desiste da negociação |
| POST | `/propostas/{id}/cancelar` | Cancela `{justificativa}` (obrigatória na contraproposta; a raiz leva as contrapropostas junto) |
| PATCH | `/propostas/{id}/finalizar` | Raiz → FINALIZADA e dispara o `TrocaConcluidaEvent` |
| GET | `/avaliacoes` (`?propostaId=`, `?usuarioAlvoId=`) | Lista avaliações |
| PATCH | `/avaliacoes/{id}` | Preenche `{nota, descricao}` (+ `usuarioId` opcional) |
| GET | `/historicos?propostaId=` | Histórico de uma proposta |
| GET | `/logs?tipo=` · `/logs/nivel?nivel=ERROR&horas=24` | LogEvento no Mongo |

Todos os erros seguem o [contrato de erro](#entrega-final--aula-11-contrato-de-erro):
ProblemDetail com `status`, `codigo` estável e `detail`.

Ferramentas de aula: `GET /demo/erro-inesperado` força um 500 (Aula 11);
`cenario-final.sh` (do professor), `preparar-cenario.sh` e `corrida.sh` reproduzem as corridas (Aula 10).

## Testando a API

O arquivo `COLLECTION_INSOMNIA.json` na raiz do projeto tem as requisições
de exemplo acima prontas para importar no Insomnia (ou no Postman, que
também importa esse formato). Ele já vem com uma variável de ambiente
`base_url` apontando para `http://localhost:8090` (Docker Compose).

## Testes automatizados

Nesta fase, só existe um teste de contexto do Spring Boot (confirma que a
aplicação sobe sem erros de configuração). Como ainda não configuramos um
banco em memória para os testes (fora de escopo por enquanto), o Postgres
precisa estar no ar para o contexto carregar:

```bash
docker compose up db mongo -d
./mvnw test
```
