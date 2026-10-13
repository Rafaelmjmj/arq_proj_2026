package com.trokr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Preenchimento de uma Avaliacao pendente. usuarioId é quem está avaliando
 * (precisa ser o autor da avaliação) — substitui o "usuário logado"
 * enquanto não existe autenticação.
 */
public record AvaliarRequestDTO(

        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @NotNull(message = "nota é obrigatória")
        @Min(value = 1, message = "nota mínima é 1")
        @Max(value = 5, message = "nota máxima é 5")
        Integer nota,

        @Size(max = 1000, message = "descricao pode ter no máximo 1000 caracteres")
        String descricao
) {
}
