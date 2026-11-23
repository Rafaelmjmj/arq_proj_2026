package com.trokr.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.util.List;
import org.junit.jupiter.api.Test;

/** O catálogo montado à mão com as três fábricas, como o Spring faria. */
class CatalogoDeFabricasTest {

    private final CatalogoDeFabricas catalogo = new CatalogoDeFabricas(List.of(
            new FabricaItemProduto(), new FabricaItemServico(), new FabricaItemExperiencia()));

    private final Usuario ana = Fixtures.usuario(1, "Ana");

    private static ItemRequestDTO dto(CategoriaItem categoria, Integer peso, Integer duracao, NivelExperiencia nivel) {
        return new ItemRequestDTO("Título", "Descrição", categoria, 1L, peso, duracao, nivel);
    }

    @Test
    void escolheAFabricaPelaCategoria() {
        assertInstanceOf(ItemProduto.class, catalogo.criar(dto(CategoriaItem.PRODUTO, 500, null, null), ana));
        assertInstanceOf(ItemServico.class, catalogo.criar(dto(CategoriaItem.SERVICO, null, 30, null), ana));
        assertInstanceOf(ItemExperiencia.class,
                catalogo.criar(dto(CategoriaItem.EXPERIENCIA, null, null, NivelExperiencia.AVANCADO), ana));
    }

    @Test
    void atualizacaoNaoMudaOTipo() {
        Item produto = Fixtures.item(1, "Bicicleta", CategoriaItem.PRODUTO, ana);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> catalogo.validarAtualizacao(produto, dto(CategoriaItem.SERVICO, null, 30, null)));
        assertEquals("ITEM_CATEGORIA_IMUTAVEL", ex.getCodigo());
    }

    @Test
    void atualizacaoConfereOsCamposDoTipo() {
        Item produto = Fixtures.item(1, "Bicicleta", CategoriaItem.PRODUTO, ana);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> catalogo.validarAtualizacao(produto, dto(CategoriaItem.PRODUTO, null, null, null)));
        assertEquals("ITEM_DADOS_INCOMPLETOS", ex.getCodigo());
    }

    @Test
    void atualizacaoValidaAplicaOsNovosDados() {
        Item produto = Fixtures.item(1, "Bicicleta", CategoriaItem.PRODUTO, ana);
        ItemRequestDTO novo = dto(CategoriaItem.PRODUTO, 12000, null, null);

        catalogo.validarAtualizacao(produto, novo);
        produto.atualizar("Bicicleta aro 29", "Revisada", ana, novo.dadosDoTipo());

        assertEquals("Bicicleta aro 29", produto.getTitulo());
        assertEquals(12000, produto.dadosDoTipo().pesoGramas());
    }

    @Test
    void duasFabricasParaAMesmaCategoriaEDefeito() {
        assertThrows(IllegalStateException.class, () -> new CatalogoDeFabricas(List.of(
                new FabricaItemProduto(), new FabricaItemProduto())));
    }

    @Test
    void categoriaSemFabricaEDefeito() {
        CatalogoDeFabricas incompleto = new CatalogoDeFabricas(List.of(new FabricaItemProduto()));
        assertThrows(IllegalStateException.class,
                () -> incompleto.criar(dto(CategoriaItem.SERVICO, null, 30, null), ana));
    }
}
