package com.trokr.factory;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.Item;
import com.trokr.model.ItemServico;
import com.trokr.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Fábrica de ItemServico (Aula 12). Serviço: exige duracaoMinutos; rejeita pesoGramas e nivel.
 *
 * A fábrica decide e valida; o builder ({@code @SuperBuilder}) monta.
 */
@Component
public class FabricaItemServico implements FabricaDeItem {

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.SERVICO;
    }

    @Override
    public void validar(DadosDoTipo dados) {
        RegrasDeItem.exigir(dados.duracaoMinutos(), "duracaoMinutos", categoria());
        RegrasDeItem.rejeitar(dados.pesoGramas(), "pesoGramas", categoria());
        RegrasDeItem.rejeitar(dados.nivel(), "nivel", categoria());
    }

    @Override
    public Item criar(ItemRequestDTO dados, Usuario proprietario) {
        validar(dados.dadosDoTipo());
        return ItemServico.builder()
                .titulo(dados.titulo())
                .descricao(dados.descricao())
                .usuarioProprietario(proprietario)
                .duracaoMinutos(dados.duracaoMinutos())
                .build();
    }
}
