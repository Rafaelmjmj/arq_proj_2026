package com.trokr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.Item;
import com.trokr.model.NivelExperiencia;
import java.time.LocalDateTime;

/**
 * Dados de saída de um Item, com o dono achatado (usuarioId/usuarioNome).
 *
 * Aula 12: cada item devolve só os campos do SEU tipo — um produto aparece
 * com "pesoGramas" e sem "nivel": null. Os campos vêm de
 * {@code item.dadosDoTipo()}, que é polimórfico: nenhum instanceof aqui.
 */
public record ItemResponseDTO(
        Long id,
        String titulo,
        String descricao,
        CategoriaItem categoria,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime dataCriacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer pesoGramas,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer duracaoMinutos,
        @JsonInclude(JsonInclude.Include.NON_NULL) NivelExperiencia nivel
) {

    public static ItemResponseDTO fromEntity(Item item) {
        DadosDoTipo dados = item.dadosDoTipo();
        return new ItemResponseDTO(
                item.getId(),
                item.getTitulo(),
                item.getDescricao(),
                item.getCategoria(),
                item.getUsuarioProprietario().getId(),
                item.getUsuarioProprietario().getNome(),
                item.getDataCriacao(),
                dados.pesoGramas(),
                dados.duracaoMinutos(),
                dados.nivel()
        );
    }
}
