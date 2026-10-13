# Trokr

Plataforma de trocas colaborativas: usuários cadastram itens ou habilidades
que oferecem e propõem trocas entre si, sem dinheiro envolvido.

> **"Trokr" é um nome de trabalho.** A turma vai batizar o projeto na Aula 1.
> O nome só aparece em lugares fáceis de trocar depois (pacote Java, nome do
> projeto Maven, nomes de containers/banco no Docker) — não está espalhado
> pela lógica de negócio.

## Entrega 2 — onde está cada padrão

| Padrão | Onde está | O que mostrar na arguição |
|---|---|---|
| **Domain Model rico** | `model/Proposta`, `model/Avaliacao`, `model/Usuario` | Não há `setStatus()`: o status só muda por métodos de negócio (`submeter`, `aceitar`, `finalizar`...). Regras como "só o dono da raiz aceita" e "só propostas Ativas recebem contraproposta" moram na entidade. |
| **State** (Opção A) | `model/state/proposta` (6 estados da raiz) e `model/state/contraproposta` (6 da contraproposta) | Duas interfaces independentes. Cada estado decide as próprias transições e lança `IllegalStateException` (HTTP 409) para as inválidas. O efeito cascata (`encerrarPorFimDaRaiz`) também é decidido por cada estado. |
| **Observer** | `event/TrocaConcluidaEvent` + `listener/*` (5 listeners) | O `PropostaService` só publica o evento, e somente quando a troca chega ao estado final. Os listeners não se conhecem: incluir um sexto não mexe em nada que já existe. |
| **Adapter** | `adapter/RegistroDeEventos` (porta) + `adapter/mongo/RegistroDeEventosMongo` (adaptador) | O `LogEventoService` depende só da interface. O documento Mongo (`LogEvento`) nunca sai do pacote do adaptador. |
| **Strategy** | `strategy/EstrategiaCredito` + `CreditoProduto`, `CreditoServico`, `CreditoExperiencia`; contexto em `service/CalculadoraCreditoService` | Um `Map<CategoriaItem, EstrategiaCredito>` montado pelo Spring, sem nenhum switch por categoria. Cada estratégia tem teste unitário próprio, sem Spring. |
| **Facade** | `service/PropostaService` | O controller só conhece o `PropostaService`. `finalizarNegociacao()` coordena o State, a persistência e o evento. O Javadoc da classe explica a ordem e o que ela não faz. |

Créditos por categoria (constante `CREDITOS` em cada estratégia): PRODUTO = 10,
SERVICO = 15, EXPERIENCIA = 20.

Diagramas: `diagramas/DiagramaDePadroesEntrega2.png` (visão da Entrega 2),
`DiagramaDeClasseV2.png` (completo) e os dois diagramas de estado V2. Os
fontes PlantUML são os `.txt` da mesma pasta.

### Roteiro de teste (Aula 9)

Importe `COLLECTION_INSOMNIA.json` e rode a pasta **"0. Roteiro Aula 9"** em
ordem, com o banco zerado (`docker compose down -v` e depois
`docker compose up --build`). Os passos marcados "ERRO esperado" precisam
falhar. Ao fim, `GET /usuarios` mostra A = 10 e B = 15 créditos, e existem
2 avaliações pendentes, 1 histórico e 1 log `TROCA_CONCLUIDA`.

### Testes unitários

`./mvnw test -Dtest='PropostaStateTest,AvaliacaoTest,Credito*Test,CalculadoraCreditoServiceTest'`
roda os testes de domínio, State e Strategy sem precisar de banco. O
`TrokrApplicationTests` continua exigindo Postgres e Mongo no ar.

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
- **`Item` é uma entidade genérica** — ainda não existe distinção entre
  produto físico, serviço ou aula, e não existe conceito de "Proposta de
  Troca" ligando dois itens. Isso é conteúdo de aulas futuras.
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
  "usuarioId": 1
}
```

**Response — `201 Created`**

```json
{
  "id": 1,
  "titulo": "Aula de violão",
  "descricao": "Aula básica de 1 hora, para iniciantes",
  "usuarioId": 1,
  "usuarioNome": "Maria Silva",
  "dataCriacao": "2026-08-02T21:19:06.836283"
}
```

**Erros comuns**

- `400 Bad Request`: dados de entrada inválidos (ex.: `email` fora do
  formato, campo obrigatório em branco)
- `404 Not Found`: id de `usuario`/`item` inexistente, incluindo quando
  `usuarioId` informado ao criar/atualizar um item não existe

```json
{ "erro": "Usuario não encontrado com id 999" }
```

### Propostas e contrapropostas (Entrega 2)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/propostas` (`?apenasRaizes=true`) | Lista propostas |
| GET | `/propostas/{id}` | Busca uma proposta |
| GET | `/propostas/{id}/contrapropostas` | Contrapropostas concorrentes de uma raiz |
| POST | `/propostas` | Cria proposta raiz `{titulo, descricao, usuarioId, itemId}` |
| POST | `/propostas/{id}/contrapropostas` | Contrapropõe `{descricao, usuarioId, itemId}` |
| PATCH | `/propostas/{id}/submeter` | Raiz → Homologação · Contra → Em Análise |
| PATCH | `/propostas/{id}/aprovar` | Raiz: Homologação → Ativa |
| PATCH | `/propostas/{id}/editar` | Volta para Rascunho |
| PATCH | `/propostas/{id}/aceitar?usuarioId=` | Dono da raiz aceita a contraproposta |
| PATCH | `/propostas/{id}/recusar?usuarioId=` | Dono da raiz recusa a contraproposta em análise |
| PATCH | `/propostas/{id}/desistir?usuarioId=` | Uma das partes desiste da negociação |
| PATCH | `/propostas/{id}/cancelar` | Cancela (a raiz leva as contrapropostas junto) |
| PATCH | `/propostas/{id}/finalizar` | Conclui a troca e dispara o `TrocaConcluidaEvent` |
| GET | `/avaliacoes` (`?propostaId=`, `?usuarioAlvoId=`) | Lista avaliações |
| PATCH | `/avaliacoes/{id}` | Preenche `{usuarioId, nota, descricao}` |
| GET | `/historicos?propostaId=` | Histórico de uma proposta |
| GET | `/logs?tipo=` · `/logs/nivel?nivel=ERROR&horas=24` | LogEvento no Mongo |

Códigos de erro: 400 (entrada inválida), 403 (agir em nome de outra pessoa),
404 (não encontrado), 409 (transição inválida ou conflito).

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
