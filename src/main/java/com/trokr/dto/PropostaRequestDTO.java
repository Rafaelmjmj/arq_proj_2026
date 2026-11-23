package com.trokr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar uma Proposta RAIZ. O usuário informa o próprio
 * id (ainda não existe autenticação) e o item que está oferecendo.
 */
public record PropostaRequestDTO(

        @Schema(description = "Título da proposta", example = "Troco bicicleta")
        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        @Schema(description = "O que se procura em troca", example = "Aceito instrumento musical ou aulas")
        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @Schema(description = "Autor da proposta", example = "1")
        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @Schema(description = "Item oferecido (precisa ser do autor)", example = "1")
        @NotNull(message = "itemId é obrigatório")
        Long itemId
) {
}
