package com.trokr.dto;

import com.trokr.model.Avaliacao;
import com.trokr.model.StatusAvaliacao;
import java.time.LocalDateTime;

public record AvaliacaoResponseDTO(
        Long id,
        StatusAvaliacao status,
        Integer nota,
        String descricao,
        Long propostaId,
        Long usuarioAutorId,
        String usuarioAutorNome,
        Long usuarioAlvoId,
        String usuarioAlvoNome,
        LocalDateTime dataCriacao,
        LocalDateTime dataAvaliacao
) {

    public static AvaliacaoResponseDTO fromEntity(Avaliacao avaliacao) {
        return new AvaliacaoResponseDTO(
                avaliacao.getId(),
                avaliacao.getStatus(),
                avaliacao.getNota(),
                avaliacao.getDescricao(),
                avaliacao.getProposta().getId(),
                avaliacao.getUsuarioAutor().getId(),
                avaliacao.getUsuarioAutor().getNome(),
                avaliacao.getUsuarioAlvo().getId(),
                avaliacao.getUsuarioAlvo().getNome(),
                avaliacao.getDataCriacao(),
                avaliacao.getDataAvaliacao()
        );
    }
}
