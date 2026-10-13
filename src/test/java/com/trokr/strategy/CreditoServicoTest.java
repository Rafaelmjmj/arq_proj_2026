package com.trokr.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Item;
import org.junit.jupiter.api.Test;

/** Estratégia testada isolada: new CreditoServico() e um Item montado à mão, sem Spring. */
class CreditoServicoTest {

    private final CreditoServico estrategia = new CreditoServico();

    @Test
    void atendeACategoriaServico() {
        assertEquals(CategoriaItem.SERVICO, estrategia.categoria());
    }

    @Test
    void calculaOsCreditosDaCategoria() {
        Item item = Fixtures.item(1, "Aula de violão", CategoriaItem.SERVICO, Fixtures.usuario(1, "Ana"));
        assertEquals(CreditoServico.CREDITOS, estrategia.calcular(item));
    }
}
