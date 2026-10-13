package com.trokr.model;

/**
 * Aula 9 — categoria de um Item. Por enquanto é só um rótulo: indica qual
 * EstrategiaCredito calcula os créditos do item (Strategy).
 * Subclasses de Item com campos próprios ficam para a Aula 14 (Factory/Builder).
 */
public enum CategoriaItem {
    PRODUTO,
    SERVICO,
    EXPERIENCIA
}
