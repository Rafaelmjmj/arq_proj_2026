package com.trokr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar/atualizar um Item.
 * O item precisa informar o id do usuário dono no corpo da requisição,
 * já que ainda não existe autenticação para inferir isso a partir de quem
 * está logado.
 */
public record ItemRequestDTO(

        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId
) {
}
