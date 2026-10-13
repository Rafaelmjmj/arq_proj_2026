package com.trokr.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Item;
import org.junit.jupiter.api.Test;

/** Estratégia testada isolada: new CreditoExperiencia() e um Item montado à mão, sem Spring. */
class CreditoExperienciaTest {

    private final CreditoExperiencia estrategia = new CreditoExperiencia();

    @Test
    void atendeACategoriaExperiencia() {
        assertEquals(CategoriaItem.EXPERIENCIA, estrategia.categoria());
    }

    @Test
    void calculaOsCreditosDaCategoria() {
        Item item = Fixtures.item(1, "Passeio guiado", CategoriaItem.EXPERIENCIA, Fixtures.usuario(1, "Ana"));
        assertEquals(CreditoExperiencia.CREDITOS, estrategia.calcular(item));
    }
}
