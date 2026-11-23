package com.trokr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "Autor da avaliação (quem preenche)", example = "1")
        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @Schema(description = "De 1 a 5", example = "5")
        @NotNull(message = "nota é obrigatória")
        @Min(value = 1, message = "nota mínima é 1")
        @Max(value = 5, message = "nota máxima é 5")
        Integer nota,

        @Schema(description = "Comentário opcional", example = "Troca tranquila, recomendo")
        @Size(max = 1000, message = "descricao pode ter no máximo 1000 caracteres")
        String descricao
) {
}
