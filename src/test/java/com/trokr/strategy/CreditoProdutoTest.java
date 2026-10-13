package com.trokr.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Item;
import org.junit.jupiter.api.Test;

/** Estratégia testada isolada: new CreditoProduto() e um Item montado à mão, sem Spring. */
class CreditoProdutoTest {

    private final CreditoProduto estrategia = new CreditoProduto();

    @Test
    void atendeACategoriaProduto() {
        assertEquals(CategoriaItem.PRODUTO, estrategia.categoria());
    }

    @Test
    void calculaOsCreditosDaCategoria() {
        Item item = Fixtures.item(1, "Bicicleta", CategoriaItem.PRODUTO, Fixtures.usuario(1, "Ana"));
        assertEquals(CreditoProduto.CREDITOS, estrategia.calcular(item));
    }
}
