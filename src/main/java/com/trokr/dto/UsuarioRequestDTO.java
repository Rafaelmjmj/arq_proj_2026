package com.trokr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados de entrada para criar/atualizar um Usuario.
 * Um record simples resolve bem esse caso — sem necessidade de biblioteca
 * de mapeamento (MapStruct etc.), a conversão para/de entidade é manual.
 */
public record UsuarioRequestDTO(

        @Schema(description = "Nome no perfil", example = "Ana Souza")
        @NotBlank(message = "nome é obrigatório")
        String nome,

        @Schema(description = "E-mail único de contato", example = "ana@trokr.dev")
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ter um formato válido")
        String email,

        @Schema(description = "Cidade, para trocas presenciais", example = "Formosa")
        @NotBlank(message = "cidade é obrigatório")
        String cidade
) {
}
