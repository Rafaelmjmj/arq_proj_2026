package com.trokr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar uma CONTRAPROPOSTA. A proposta original vem
 * na URL (POST /propostas/{id}/contrapropostas); o título é herdado dela.
 */
public record ContrapropostaRequestDTO(

        @Schema(description = "O que se oferece em troca", example = "Ofereço 4 aulas de violão")
        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @Schema(description = "Autor da contraproposta", example = "2")
        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @Schema(description = "Item oferecido (precisa ser do autor)", example = "2")
        @NotNull(message = "itemId é obrigatório")
        Long itemId
) {
}
