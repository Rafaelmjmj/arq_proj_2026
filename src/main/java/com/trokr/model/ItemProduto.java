package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Produto físico (bicicleta, livro, violão). Quem recebe precisa saber o peso
 * para decidir se leva no ônibus ou precisa de frete.
 *
 * Uma linha na tabela "item" com categoria = PRODUTO. A coluna peso_gramas aceita
 * nulo no banco (SINGLE_TABLE: as linhas dos outros tipos não a preenchem);
 * quem garante que todo produto tenha pesoGramas é a fábrica deste tipo.
 */
@Entity
@DiscriminatorValue("PRODUTO")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemProduto extends Item {

    @Column(name = "peso_gramas")
    private Integer pesoGramas;

    @Override
    public CategoriaItem getCategoria() {
        return CategoriaItem.PRODUTO;
    }

    @Override
    public DadosDoTipo dadosDoTipo() {
        return new DadosDoTipo(pesoGramas, null, null);
    }

    @Override
    protected void aplicarDadosDoTipo(DadosDoTipo dados) {
        this.pesoGramas = dados.pesoGramas();
    }
}
