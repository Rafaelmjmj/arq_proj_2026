package com.trokr.factory;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.Item;
import com.trokr.model.ItemExperiencia;
import com.trokr.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Fábrica de ItemExperiencia (Aula 12). Experiência: exige nivel; rejeita pesoGramas e duracaoMinutos.
 *
 * A fábrica decide e valida; o builder ({@code @SuperBuilder}) monta.
 */
@Component
public class FabricaItemExperiencia implements FabricaDeItem {

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.EXPERIENCIA;
    }

    @Override
    public void validar(DadosDoTipo dados) {
        RegrasDeItem.exigir(dados.nivel(), "nivel", categoria());
        RegrasDeItem.rejeitar(dados.pesoGramas(), "pesoGramas", categoria());
        RegrasDeItem.rejeitar(dados.duracaoMinutos(), "duracaoMinutos", categoria());
    }

    @Override
    public Item criar(ItemRequestDTO dados, Usuario proprietario) {
        validar(dados.dadosDoTipo());
        return ItemExperiencia.builder()
                .titulo(dados.titulo())
                .descricao(dados.descricao())
                .usuarioProprietario(proprietario)
                .nivel(dados.nivel())
                .build();
    }
}
