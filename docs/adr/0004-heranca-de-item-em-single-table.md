# 0004. Mapear a herança de Item em SINGLE_TABLE

## Status
Aceito (Aula 12).

## Contexto
Os tipos de oferta passaram a ter dados próprios: produto tem peso, serviço
tem duração, experiência tem nível. Uma classe só com os três campos não
impede um serviço com peso, e cada regra viraria um `if` por categoria. O
banco já tinha a tabela `item` com a coluna `categoria` (Aula 9), com dados,
e a Proposta referencia `Item` (não o subtipo).

## Alternativas consideradas
- **JOINED**: uma tabela por subclasse, colunas NOT NULL de verdade. Perdeu
  porque toda leitura de item vira um JOIN, e os dados que já estavam em
  `item` precisariam de migração para as tabelas novas.
- **TABLE_PER_CLASS**: sem tabela-mãe; uma consulta polimórfica (a Proposta
  apontando para "qualquer Item") vira UNION, e o id não pode ser IDENTITY.
- **Um campo JSON / Map com os atributos**: flexível, mas o compilador não
  ajuda ("peso" escrito errado só aparece em produção).

## Decisão
`Item` é abstrato e mapeado em SINGLE_TABLE, com a coluna `categoria` como
discriminador; `ItemProduto`, `ItemServico` e `ItemExperiencia` são as
subclasses, criadas pelas fábricas do `CatalogoDeFabricas`.

## Consequências
- \+ Uma tabela, sem JOIN; os itens antigos carregam como a subclasse certa.
- \+ A Strategy de créditos não mudou: `getCategoria()` virou polimórfico.
- \- `peso_gramas`, `duracao_minutos` e `nivel` aceitam nulo no banco: a
  garantia "todo produto tem peso" é do código (as fábricas), não do banco.
- \- Com LAZY, o Hibernate entrega um proxy de `Item`: `instanceof` não é
  confiável, e quem precisa do tipo pergunta ao objeto (`dadosDoTipo()`).
