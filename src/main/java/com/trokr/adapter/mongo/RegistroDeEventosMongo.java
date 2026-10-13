package com.trokr.adapter.mongo;

import com.trokr.adapter.EventoRegistrado;
import com.trokr.adapter.NivelLog;
import com.trokr.adapter.RegistroDeEventos;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter — o ADAPTADOR CONCRETO para MongoDB.
 *
 * Traduz o vocabulário genérico da porta ({@link RegistroDeEventos}) para a
 * API real do Spring Data MongoDB: registrar() vira insert() de um
 * documento {@link LogEvento}; buscarPorTipo() vira um query method e o
 * documento é convertido de volta para {@link EventoRegistrado}.
 */
@Component
@RequiredArgsConstructor
public class RegistroDeEventosMongo implements RegistroDeEventos {

    private final LogEventoRepository logEventoRepository;

    @Override
    public EventoRegistrado registrar(EventoRegistrado evento) {
        LogEvento salvo = logEventoRepository.insert(paraDocumento(evento));
        return paraEvento(salvo);
    }

    @Override
    public List<EventoRegistrado> buscarPorTipo(String tipo) {
        return logEventoRepository.findByTipoOrderByTimestampDesc(tipo).stream()
                .map(this::paraEvento)
                .toList();
    }

    @Override
    public List<EventoRegistrado> buscarPorNivelDesde(NivelLog nivel, LocalDateTime desde) {
        return logEventoRepository.findByNivelAndTimestampAfterOrderByTimestampDesc(nivel, desde).stream()
                .map(this::paraEvento)
                .toList();
    }

    // --- tradução domínio <-> documento Mongo ---

    private LogEvento paraDocumento(EventoRegistrado evento) {
        LogEvento documento = new LogEvento();
        documento.setTipo(evento.tipo());
        documento.setNivel(evento.nivel());
        documento.setOrigem(evento.origem());
        documento.setMensagem(evento.mensagem());
        documento.setPayload(evento.payload());
        documento.setTimestamp(evento.timestamp());
        documento.setUsuarioId(evento.usuarioId());
        return documento;
    }

    private EventoRegistrado paraEvento(LogEvento documento) {
        return new EventoRegistrado(
                documento.getId(),
                documento.getTipo(),
                documento.getNivel(),
                documento.getOrigem(),
                documento.getMensagem(),
                documento.getPayload(),
                documento.getTimestamp(),
                documento.getUsuarioId()
        );
    }
}
