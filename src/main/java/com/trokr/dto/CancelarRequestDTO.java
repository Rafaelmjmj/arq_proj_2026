package com.trokr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo de POST /propostas/{id}/cancelar. A justificativa é obrigatória para
 * cancelar uma contraproposta (422 JUSTIFICATIVA_OBRIGATORIA) e opcional para
 * a proposta raiz. A regra mora na Proposta, não aqui: um @NotBlank daria 400,
 * e o roteiro espera 422 (regra de negócio).
 */
public record CancelarRequestDTO(
        @Schema(description = "Motivo do cancelamento (obrigatório para contraproposta)",
                example = "Encontrei outra troca")
        String justificativa
) {
}
