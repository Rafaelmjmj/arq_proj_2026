# Architecture Decision Records do Trokr

Uma decisão por arquivo, numerada em sequência; o número nunca é
reaproveitado. Um ADR não se reescreve: se a decisão mudar, um ADR novo a
substitui e o antigo ganha o status "Substituído por 000N".

Modelo (Nygard + "Alternativas consideradas" do MADR):

```
# 000N. Título curto, no imperativo
## Status        Proposto | Aceito | Substituído por 000M (e a data)
## Contexto      Qual problema? Que forças puxam para lados diferentes?
## Alternativas consideradas   O que mais estava na mesa, e por que perdeu?
## Decisão       O que decidimos, em uma ou duas frases afirmativas.
## Consequências O que fica mais fácil? O que fica mais difícil?
```

| ADR | Decisão | Aula |
|---|---|---|
| [0001](0001-postgres-para-o-negocio-mongodb-para-o-log.md) | Postgres para o negócio, MongoDB para o log de eventos | 8 |
| [0002](0002-bloqueio-otimista-com-version.md) | Bloqueio otimista com `@Version` na Proposta | 10 |
| [0003](0003-disjuntor-escrito-a-mao.md) | Retry e disjuntor escritos à mão, como Decorators | 13 |
| [0004](0004-heranca-de-item-em-single-table.md) | Herança de Item em SINGLE_TABLE | 12 |
| [0005](0005-monolito-modular.md) | Monolito modular em vez de microsserviços | 14 |
