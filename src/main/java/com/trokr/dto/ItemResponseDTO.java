package com.trokr.dto;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import java.time.LocalDateTime;

/** Dados de saída de um Item, com o dono achatado (usuarioId/usuarioNome). */
public record ItemResponseDTO(
        Long id,
        String titulo,
        String descricao,
        CategoriaItem categoria,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime dataCriacao
) {

    public static ItemResponseDTO fromEntity(Item item) {
        return new ItemResponseDTO(
                item.getId(),
                item.getTitulo(),
                item.getDescricao(),
                item.getCategoria(),
                item.getUsuarioProprietario().getId(),
                item.getUsuarioProprietario().getNome(),
                item.getDataCriacao()
        );
    }
}
