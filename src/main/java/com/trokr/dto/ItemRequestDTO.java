package com.trokr.dto;

import com.trokr.model.CategoriaItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de entrada para criar/atualizar um Item. A categoria é obrigatória
 * (Aula 9): é ela que decide a estratégia de crédito.
 * Valores aceitos: PRODUTO, SERVICO, EXPERIENCIA.
 */
public record ItemRequestDTO(

        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @NotNull(message = "categoria é obrigatória (PRODUTO, SERVICO ou EXPERIENCIA)")
        CategoriaItem categoria,

        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId
) {
}
