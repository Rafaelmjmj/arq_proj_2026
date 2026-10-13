package com.trokr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Item;
import com.trokr.model.Usuario;
import com.trokr.strategy.CreditoExperiencia;
import com.trokr.strategy.CreditoProduto;
import com.trokr.strategy.CreditoServico;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * O contexto da Strategy, montado à mão com a mesma lista que o Spring
 * injetaria. Confere que cada categoria cai na estratégia certa.
 */
class CalculadoraCreditoServiceTest {

    private final Usuario ana = Fixtures.usuario(1, "Ana");

    @Test
    void escolheAEstrategiaPelaCategoriaDoItem() {
        CalculadoraCreditoService calculadora = new CalculadoraCreditoService(
                List.of(new CreditoProduto(), new CreditoServico(), new CreditoExperiencia()));

        assertEquals(CreditoProduto.CREDITOS, calculadora.calcular(item(CategoriaItem.PRODUTO)));
        assertEquals(CreditoServico.CREDITOS, calculadora.calcular(item(CategoriaItem.SERVICO)));
        assertEquals(CreditoExperiencia.CREDITOS, calculadora.calcular(item(CategoriaItem.EXPERIENCIA)));
    }

    @Test
    void falhaComMensagemClaraSeFaltarEstrategia() {
        CalculadoraCreditoService soProduto = new CalculadoraCreditoService(List.of(new CreditoProduto()));
        assertThrows(IllegalStateException.class, () -> soProduto.calcular(item(CategoriaItem.SERVICO)));
    }

    @Test
    void duasEstrategiasParaAMesmaCategoriaImpedemASubida() {
        assertThrows(IllegalStateException.class,
                () -> new CalculadoraCreditoService(List.of(new CreditoProduto(), new CreditoProduto())));
    }

    private Item item(CategoriaItem categoria) {
        return Fixtures.item(1, "item", categoria, ana);
    }
}
