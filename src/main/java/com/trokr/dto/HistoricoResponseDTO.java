package com.trokr.dto;

import com.trokr.model.Historico;
import com.trokr.model.Status;
import java.time.LocalDateTime;

public record HistoricoResponseDTO(
        Long id,
        Long propostaId,
        String motivo,
        Status statusAnterior,
        Status statusNovo,
        LocalDateTime data,
        Long usuarioId
) {

    public static HistoricoResponseDTO fromEntity(Historico historico) {
        return new HistoricoResponseDTO(
                historico.getId(),
                historico.getProposta().getId(),
                historico.getMotivo(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getData(),
                historico.getUsuario() == null ? null : historico.getUsuario().getId()
        );
    }
}
