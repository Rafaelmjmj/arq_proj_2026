package com.trokr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar uma Proposta RAIZ. O usuário informa o próprio
 * id (ainda não existe autenticação) e o item que está oferecendo.
 */
public record PropostaRequestDTO(

        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @NotNull(message = "itemId é obrigatório")
        Long itemId
) {
}
