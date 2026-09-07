package com.trokr.dto;

import com.trokr.model.Usuario;
import java.time.LocalDateTime;

/**
 * Dados de saída de um Usuario. Nunca devolvemos a entidade JPA diretamente
 * pela API — isso evita expor detalhes de persistência e dá liberdade para
 * o formato de entrada/saída evoluir sem quebrar o modelo de dados.
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        LocalDateTime dataCriacao
) {

    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getDataCriacao()
        );
    }
}
