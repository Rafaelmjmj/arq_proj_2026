package com.trokr.model;

/**
 * Categoria de um Item.
 *
 * Aula 9: indica qual EstrategiaCredito calcula os créditos do item (Strategy).
 * Aula 12: é também o valor do discriminador da herança (coluna "categoria"):
 * PRODUTO → ItemProduto, SERVICO → ItemServico, EXPERIENCIA → ItemExperiencia.
 * Cada subclasse devolve a sua em getCategoria(), e a categoria escolhe a
 * fábrica que cria o item.
 */
public enum CategoriaItem {
    PRODUTO,
    SERVICO,
    EXPERIENCIA
}
