package com.trokr.dto;

import com.trokr.adapter.EventoRegistrado;
import com.trokr.adapter.NivelLog;
import java.time.LocalDateTime;
import java.util.Map;

/** Saída de GET /logs — convertida do EventoRegistrado (nunca do documento Mongo). */
public record LogEventoResponseDTO(
        String id,
        String tipo,
        NivelLog nivel,
        String origem,
        String mensagem,
        Map<String, Object> payload,
        LocalDateTime timestamp,
        Long usuarioId
) {

    public static LogEventoResponseDTO fromEvento(EventoRegistrado evento) {
        return new LogEventoResponseDTO(
                evento.id(),
                evento.tipo(),
                evento.nivel(),
                evento.origem(),
                evento.mensagem(),
                evento.payload(),
                evento.timestamp(),
                evento.usuarioId()
        );
    }
}
