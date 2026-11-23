package com.trokr.model;

/**
 * Aula 12 — os campos que variam por tipo de oferta, juntos num valor só.
 *
 * Cada subclasse de Item preenche apenas o que é seu e devolve o resto nulo
 * (um produto tem peso, não tem duração nem nível). É o que a fábrica
 * confere na criação e o que a resposta da API mostra.
 *
 * Os nomes carregam a unidade (gramas, minutos) para ninguém adivinhar.
 */
public record DadosDoTipo(
        Integer pesoGramas,
        Integer duracaoMinutos,
        NivelExperiencia nivel
) {
}
