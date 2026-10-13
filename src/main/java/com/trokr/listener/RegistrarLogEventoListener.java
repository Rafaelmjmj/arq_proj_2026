package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.service.LogEventoService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 4 de 5 (Aula 8) — grava um LogEvento TROCA_CONCLUIDA no MongoDB.
 *
 * Só conhece o LogEventoService, que por sua vez só conhece a porta
 * RegistroDeEventos (Adapter). Este listener nem sabe que existe Mongo.
 */
@Component
@RequiredArgsConstructor
public class RegistrarLogEventoListener {

    public static final String TIPO = "TROCA_CONCLUIDA";

    private final LogEventoService logEventoService;

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propostaId", evento.propostaId());
        payload.put("usuarioAId", evento.usuarioA().getId());
        payload.put("usuarioBId", evento.usuarioB().getId());
        payload.put("itemAId", evento.itemA().getId());
        payload.put("itemBId", evento.itemB().getId());
        payload.put("dataConclusao", evento.dataConclusao().toString());

        logEventoService.registrarInfo(TIPO, "PropostaService", "Troca concluída", payload, null);
    }
}
