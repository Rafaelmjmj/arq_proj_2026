package com.trokr.model;

public enum Status {

    // Status das propostas

    RASCUNHO,
    HOMOLOGACAO,
    ATIVA,
    NEGOCIADO_RAIZ,
    FINALIZADA,
    CANCELADO_RAIZ,

    // Status das contrapropostas, rascunho é reaproveitado

    EM_ANALISE,
    NEGOCIADO_CONTRA,
    FINALIZADO_CONTRA,
    RECUSADO,
    CANCELADO_CONTRA
}
