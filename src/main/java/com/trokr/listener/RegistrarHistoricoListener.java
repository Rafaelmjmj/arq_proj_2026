package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.Historico;
import com.trokr.model.Proposta;
import com.trokr.model.Status;
import com.trokr.repository.HistoricoRepository;
import com.trokr.repository.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 2 de 5 — grava no Historico (Postgres) que a troca aconteceu:
 * a Proposta raiz passou de Negociado para Finalizado.
 */
@Component
@RequiredArgsConstructor
public class RegistrarHistoricoListener {

    private final HistoricoRepository historicoRepository;
    private final PropostaRepository propostaRepository;

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        Proposta proposta = propostaRepository.getReferenceById(evento.propostaId());

        Historico historico = new Historico(
                proposta,
                "Troca concluída",
                Status.NEGOCIADO_RAIZ,
                Status.FINALIZADO,
                evento.dataConclusao(),
                null
        );
        historicoRepository.save(historico);
    }
}
