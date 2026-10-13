package com.trokr.dto;

import com.trokr.model.Usuario;
import java.time.LocalDateTime;

/**
 * Dados de saída de um Usuario. Nunca devolvemos a entidade JPA diretamente
 * pela API — isso evita expor detalhes de persistência e dá liberdade para
 * o formato de entrada/saída evoluir sem quebrar o modelo de dados.
 *
 * saldoCreditos (Aula 9) aparece SÓ aqui, na saída: o UsuarioRequestDTO não
 * tem esse campo e não existe endpoint que escreva no saldo — ele só muda
 * como consequência de uma troca concluída.
 */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cidade,
        Integer saldoCreditos,
        LocalDateTime dataCriacao
) {

    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCidade(),
                usuario.getSaldoCreditos(),
                usuario.getDataCriacao()
        );
    }
}
