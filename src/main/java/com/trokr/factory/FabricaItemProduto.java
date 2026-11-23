package com.trokr.factory;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.Item;
import com.trokr.model.ItemProduto;
import com.trokr.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Fábrica de ItemProduto (Aula 12). Produto: exige pesoGramas; rejeita duracaoMinutos e nivel.
 *
 * A fábrica decide e valida; o builder ({@code @SuperBuilder}) monta.
 */
@Component
public class FabricaItemProduto implements FabricaDeItem {

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.PRODUTO;
    }

    @Override
    public void validar(DadosDoTipo dados) {
        RegrasDeItem.exigir(dados.pesoGramas(), "pesoGramas", categoria());
        RegrasDeItem.rejeitar(dados.duracaoMinutos(), "duracaoMinutos", categoria());
        RegrasDeItem.rejeitar(dados.nivel(), "nivel", categoria());
    }

    @Override
    public Item criar(ItemRequestDTO dados, Usuario proprietario) {
        validar(dados.dadosDoTipo());
        return ItemProduto.builder()
                .titulo(dados.titulo())
                .descricao(dados.descricao())
                .usuarioProprietario(proprietario)
                .pesoGramas(dados.pesoGramas())
                .build();
    }
}
