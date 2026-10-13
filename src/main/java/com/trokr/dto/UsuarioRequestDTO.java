package com.trokr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados de entrada para criar/atualizar um Usuario.
 * Um record simples resolve bem esse caso — sem necessidade de biblioteca
 * de mapeamento (MapStruct etc.), a conversão para/de entidade é manual.
 */
public record UsuarioRequestDTO(

        @NotBlank(message = "nome é obrigatório")
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ter um formato válido")
        String email,

        @NotBlank(message = "cidade é obrigatório")
        String cidade
) {
}
