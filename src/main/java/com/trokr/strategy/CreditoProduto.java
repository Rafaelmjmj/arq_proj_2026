package com.trokr.strategy;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import org.springframework.stereotype.Component;

/**
 * Estratégia de crédito para PRODUTO.
 *
 * Produto físico (bicicleta, livro, violão): valor base fixo.
 * Na Aula 14, com subclasses de Item, pode passar a considerar o estado de conservação.
 *
 * Testável sozinha com {@code new CreditoProduto()}, sem subir o Spring.
 */
@Component
public class CreditoProduto implements EstrategiaCredito {

    /** Valor decidido pela turma — ajuste aqui se a regra mudar. */
    public static final int CREDITOS = 10;

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.PRODUTO;
    }

    @Override
    public int calcular(Item item) {
        return CREDITOS;
    }
}
