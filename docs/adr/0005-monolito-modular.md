# 0005. Manter o Trokr como monolito modular

## Status
Aceito (Aula 14).

## Contexto
O Trokr tem áreas que poderiam ser serviços separados: catálogo (itens e
fábricas), negociação (propostas e máquinas de estado), usuários (reputação
e créditos), notificação e auditoria. Hoje é uma pessoa, um deploy e uma
carga pequena e homogênea. A finalização de uma troca muda proposta e saldo
na mesma transação, com `@Version` e UPDATE atômico (ADR 0002).

## Alternativas consideradas
- **Microsserviços**: deploy e escala independentes por área. Perdeu porque
  traria os três problemas que o semestre mostrou em miniatura sem nenhum
  dos benefícios: a rede falhando em toda chamada (Aula 13), a troca e os
  créditos sem transação única (precisaria de saga com compensação) e a
  idempotência de mensagens repetidas pelo retry. Além de gateway,
  rastreamento distribuído e um deploy por serviço.
- **Monolito sem fronteiras**: mais simples hoje, mas tudo enxerga tudo, e
  um corte futuro passaria pelo meio das transações.

## Decisão
Um processo e um deploy, dividido em módulos com fronteiras explícitas: o
Observer (`TrocaConcluidaEvent`) separa o que aconteceu de quem reage, e as
portas `RegistroDeEventos` e `NotificacaoPort` isolam log e notificação.

## Consequências
- \+ Uma transação ACID por troca; deploy e depuração simples.
- \+ As costuras para cortar depois já existem: notificação e auditoria
  seriam os primeiros serviços, porque já ficam atrás de portas e eventos.
- \- Tudo escala junto: se o catálogo receber mil leituras para cada
  proposta, escala-se a aplicação inteira.
- \- A disciplina das fronteiras depende de revisão de código; nada no build
  impede um módulo de chamar o repositório de outro.
