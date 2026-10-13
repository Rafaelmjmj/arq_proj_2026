package com.trokr.model;

/**
 * Aula 7 — só dois valores e uma transição (PENDENTE → AVALIADA), sem
 * ramificação. Por isso NÃO usamos State Pattern aqui: um enum e uma
 * validação simples em Avaliacao.avaliar() resolvem.
 */
public enum StatusAvaliacao {
    PENDENTE,
    AVALIADA
}
