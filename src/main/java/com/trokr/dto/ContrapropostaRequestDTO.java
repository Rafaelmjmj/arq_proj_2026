package com.trokr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar uma CONTRAPROPOSTA. A proposta original vem
 * na URL (POST /propostas/{id}/contrapropostas); o título é herdado dela.
 */
public record ContrapropostaRequestDTO(

        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @NotNull(message = "itemId é obrigatório")
        Long itemId
) {
}
