# Guia de testes e arguição — Trokr, Entrega Final

O **roteiro oficial** é o do professor (*Roteiro de Testes de Aceitação do
Trokr*), que será usado no dia da arguição. Este guia complementa: diz onde
cada bloco do roteiro mexe no código, o que conferir com atenção, o registro
de falhas (Apêndice C) já preenchido e as perguntas prováveis.

---

## 1. Antes de começar

```bash
cd ~/arq_proj_2026
docker compose down -v                                   # banco vazio: ids e saldos do roteiro dependem disso
TROKR_DEMO_ENDPOINTS=true docker compose up --build -d   # Windows: $env:TROKR_DEMO_ENDPOINTS="true"; docker compose up --build -d
docker compose logs -f app                               # esperar "Started TrokrApplication"; Ctrl+C
sudo apt install -y jq                                   # os scripts do Bloco 7 usam o jq
chmod +x cenario-final.sh corrida.sh
```

- Swagger: **http://localhost:8090/swagger-ui.html**
- Rode os casos **na ordem** do roteiro: os ids esperados (1, 2, 3...) só batem assim.
- Para o Bloco 7 com o 409 vindo do `@Version`: `TROKR_ATRASO_MS=2000 TROKR_DEMO_ENDPOINTS=true docker compose up -d`
  (sem `--build`: só recria a aplicação). Depois do bloco, suba de novo sem a variável.
- Bloco 9: deixe `docker compose logs -f app` aberto num segundo terminal e
  respeite os tempos (CT-T03 em menos de 30 s depois do CT-T02; CT-T04 e
  CT-T05 depois de pelo menos 30 s). Se reiniciar a aplicação no meio, o
  disjuntor volta FECHADO e a contagem recomeça.

---

## 2. Onde cada bloco mexe no código

| Bloco | Casos | O que está sendo testado | Onde está |
|---|---|---|---|
| 0 | CT-00 | Swagger e contrato OpenAPI | `config/OpenApiConfig`, `@Tag`/`@Operation` nos controllers |
| 1 | U01–U08 | ProblemDetail, VALIDACAO, REQUISICAO_INVALIDA | `exception/GlobalExceptionHandler`, `dto/UsuarioRequestDTO` |
| 2 | I01–I12 | Factory + Builder, herança SINGLE_TABLE | `factory/*`, `model/Item*`, `service/ItemService` |
| 3 | P01–P07 | Máquina da raiz, `@Version` crescendo | `model/state/proposta/*`, `model/Proposta` (campo `versao`) |
| 4 | C01–C08 | Máquina da contraproposta, aceite em duas etapas, justificativa | `model/state/contraproposta/*`, `Proposta.aceitar` / `aceitarContraproposta` / `cancelar` |
| 5 | F01–F06 | Facade + Observer + Strategy + Adapter | `service/PropostaService.finalizarNegociacao`, `listener/*`, `strategy/*`, `adapter/mongo/*` |
| 6 | A01–A04 | Avaliação (Domain Model rico) | `model/Avaliacao.avaliar` |
| 7 | K00–K02 | Corridas: `@Version` e UPDATE atômico | `Proposta.versao`, `UsuarioRepository.adicionarCreditos` |
| 8 | E01–E03 | Erros do protocolo em ProblemDetail | `GlobalExceptionHandler.handleExceptionInternal` |
| 9 | T01–T05 | Hexagonal + Decorator: retry e disjuntor | `notificacao/*`, `controller/DemoNotificacaoController` |
| 10 | checklist | Swagger, ADRs, README | controllers, `docs/adr/`, `README.md` |

---

## 3. Registro de falhas (Apêndice C)

O roteiro pede que se leve esta tabela para a arguição ("explicar um bug
encontrado e corrigido conta a favor"). Estas falhas foram encontradas
comparando o projeto com o roteiro, caso a caso, e corrigidas antes de rodar.
Acrescente aqui o que aparecer quando você executar.

| Caso | O que acontecia | Causa e correção | Corrigido? |
|---|---|---|---|
| CT-U01 a U03 | 400: `cidade é obrigatório` | A cidade era obrigatória no DTO e no banco. Virou opcional (`UsuarioRequestDTO`, `Usuario.cidade`). | ☑ |
| CT-U07, I07, E03 | 400 sem `codigo` (formato padrão do Spring) | Os erros do próprio Spring não passavam pelo nosso formato. `handleExceptionInternal` agora dá `REQUISICAO_INVALIDA`, title e detail em português. | ☑ |
| CT-E01, E02 | 404/405 sem `codigo` | Mesmo ponto: `ROTA_NAO_ENCONTRADA` e `METODO_NAO_SUPORTADO`. | ☑ |
| CT-P02 | 400: `titulo é obrigatório` | A proposta exigia título e descrição. Viraram opcionais; o título vem do item. | ☑ |
| CT-P02 | sem `raiz` e `versao` na resposta | `PropostaResponseDTO` ganhou `raiz`, `versao` e `propostaAnteriorId`. | ☑ |
| CT-P03 | 409 com código `PROPOSTA_SEM_NEGOCIACAO` | Finalizar conferia a contraproposta antes do estado. Agora confere o estado da raiz primeiro: 409 `PROPOSTA_TRANSICAO_INVALIDA` citando o estado. | ☑ |
| CT-P05, C05, C07 | 404 nas rotas `enviar-homologacao`, `recusar-homologacao`, `enviar`, `aceitar-contraproposta` | As ações eram PATCH com outros nomes (`/submeter`). Viraram POST com os nomes do roteiro. | ☑ |
| CT-C01 | código `CONTRAPROPOSTA_PARA_SI_MESMO` | Renomeado para `CONTRAPROPOSTA_NA_PROPRIA_PROPOSTA`. | ☑ |
| CT-C04 | 422 `ACAO_EXCLUSIVA_DA_RAIZ` | Ação da máquina errada é ação fora de hora: agora 409 `PROPOSTA_TRANSICAO_INVALIDA`. | ☑ |
| CT-C06 | cancelar não tinha corpo nem justificativa | `POST /cancelar {justificativa}`; contraproposta sem justificativa → 422 `JUSTIFICATIVA_OBRIGATORIA`. | ☑ |
| CT-C07 | aceitar levava raiz e contraproposta juntas para Negociado | Aceite em duas etapas: `/aceitar` (contra) e `/aceitar-contraproposta` (raiz). | ☑ |
| CT-F01 | status `FINALIZADO` | Renomeado para `FINALIZADA`. | ☑ |
| CT-F02 | a vencedora ia para `FINALIZADO_CONTRA` | A vencedora agora fica em `NEGOCIADO_CONTRA`; só as concorrentes são canceladas. | ☑ |
| CT-F05 | log com tipo `TROCA_CONCLUIDA` | Tipo trocado para `INFO_GERAL`. | ☑ |
| CT-A01 | 400: `usuarioId é obrigatório` | `usuarioId` virou opcional na avaliação. | ☑ |
| CT-A02 | 409 `AVALIACAO_JA_PREENCHIDA` | Agora 422 `AVALIACAO_JA_REALIZADA`; a mensagem mostrava a nota nova em vez da registrada (corrigido). | ☑ |
| CT-T01, T05 | 204 sem corpo | O interruptor responde 200 com o estado (`{"servicoNotificacao": "FORA_DO_AR"}`). | ☑ |
| CT-K00 | `corrida.sh` com `BASE` incluindo `/propostas` | Trocado pelo script do professor; `cenario-final.sh` incluído. | ☑ |
|  |  |  | ☐ |
|  |  |  | ☐ |

---

## 4. Perguntas prováveis na arguição

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
estado errado (409): é falta de permissão. No roteiro do professor não há
login, então `?usuarioId=` é opcional: só quando ele é informado a autoria é
conferida.

**Por que aprovar uma contraproposta dá 409 e não 422 (CT-C04)?**
É uma ação fora de hora: a máquina daquela proposta não tem essa transição.
Regra violada (422) é outra coisa, como ofertar item de outra pessoa.

**Por que o JSON malformado dá 400 REQUISICAO_INVALIDA e não VALIDACAO?**
VALIDACAO é o Bean Validation recusando um JSON que foi lido (nome vazio,
nota 7). REQUISICAO_INVALIDA é antes disso: o JSON nem pôde ser lido, ou um
valor não pertence ao enum. Os dois saem do `GlobalExceptionHandler`, o
segundo pelo `handleExceptionInternal`, que dá o mesmo formato aos erros do
próprio Spring (rota inexistente, método errado, "abc" no lugar do id).

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
