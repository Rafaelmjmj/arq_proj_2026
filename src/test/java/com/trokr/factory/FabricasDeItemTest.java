package com.trokr.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.exception.RegraNegocioException;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Item;
import com.trokr.model.ItemExperiencia;
import com.trokr.model.ItemProduto;
import com.trokr.model.ItemServico;
import com.trokr.model.NivelExperiencia;
import com.trokr.model.Usuario;
import org.junit.jupiter.api.Test;

/** Cada fábrica testada isolada, com new — sem Spring, sem banco (Aula 12). */
class FabricasDeItemTest {

    private final Usuario ana = Fixtures.usuario(1, "Ana");

    private static ItemRequestDTO dto(CategoriaItem categoria, Integer peso, Integer duracao, NivelExperiencia nivel) {
        return new ItemRequestDTO("Título", "Descrição", categoria, 1L, peso, duracao, nivel);
    }

    // ---------------- Produto ----------------

    @Test
    void produtoValidoSaiPronto() {
        Item item = new FabricaItemProduto().criar(dto(CategoriaItem.PRODUTO, 800, null, null), ana);

        ItemProduto produto = assertInstanceOf(ItemProduto.class, item);
        assertEquals(CategoriaItem.PRODUTO, item.getCategoria());
        assertEquals(800, produto.getPesoGramas());
        assertEquals("Título", item.getTitulo());
        assertSame(ana, item.getUsuarioProprietario());
        assertNull(item.dadosDoTipo().duracaoMinutos());
    }

    @Test
    void produtoSemPesoNaoExiste() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemProduto().criar(dto(CategoriaItem.PRODUTO, null, null, null), ana));
        assertEquals("ITEM_DADOS_INCOMPLETOS", ex.getCodigo());
    }

    @Test
    void produtoComNivelERejeitado() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemProduto().criar(dto(CategoriaItem.PRODUTO, 800, null, NivelExperiencia.AVANCADO), ana));
        assertEquals("ITEM_CAMPO_INCOMPATIVEL", ex.getCodigo());
    }

    // ---------------- Serviço ----------------

    @Test
    void servicoValidoSaiPronto() {
        Item item = new FabricaItemServico().criar(dto(CategoriaItem.SERVICO, null, 90, null), ana);

        assertEquals(90, assertInstanceOf(ItemServico.class, item).getDuracaoMinutos());
        assertEquals(CategoriaItem.SERVICO, item.getCategoria());
    }

    @Test
    void servicoSemDuracaoNaoExiste() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemServico().criar(dto(CategoriaItem.SERVICO, null, null, null), ana));
        assertEquals("ITEM_DADOS_INCOMPLETOS", ex.getCodigo());
    }

    @Test
    void servicoComPesoERejeitado() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemServico().criar(dto(CategoriaItem.SERVICO, 300, 90, null), ana));
        assertEquals("ITEM_CAMPO_INCOMPATIVEL", ex.getCodigo());
    }

    // ---------------- Experiência ----------------

    @Test
    void experienciaValidaSaiPronta() {
        Item item = new FabricaItemExperiencia().criar(dto(CategoriaItem.EXPERIENCIA, null, null, NivelExperiencia.INTERMEDIARIO), ana);

        assertEquals(NivelExperiencia.INTERMEDIARIO, assertInstanceOf(ItemExperiencia.class, item).getNivel());
        assertEquals(CategoriaItem.EXPERIENCIA, item.getCategoria());
    }

    @Test
    void experienciaSemNivelNaoExiste() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemExperiencia().criar(dto(CategoriaItem.EXPERIENCIA, null, null, null), ana));
        assertEquals("ITEM_DADOS_INCOMPLETOS", ex.getCodigo());
    }

    @Test
    void experienciaComDuracaoERejeitada() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> new FabricaItemExperiencia().criar(dto(CategoriaItem.EXPERIENCIA, null, 60, NivelExperiencia.INICIANTE), ana));
        assertEquals("ITEM_CAMPO_INCOMPATIVEL", ex.getCodigo());
    }
}
