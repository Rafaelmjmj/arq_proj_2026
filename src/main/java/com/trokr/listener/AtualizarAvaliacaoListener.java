package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.Avaliacao;
import com.trokr.model.Proposta;
import com.trokr.repository.AvaliacaoRepository;
import com.trokr.repository.PropostaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 1 de 5 — cria as DUAS avaliações pendentes, uma em cada direção
 * (A avalia B, B avalia A), sem nota nem descrição ainda.
 */
@Component
@RequiredArgsConstructor
public class AtualizarAvaliacaoListener {

    private final AvaliacaoRepository avaliacaoRepository;
    private final PropostaRepository propostaRepository;

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        Proposta proposta = propostaRepository.getReferenceById(evento.propostaId());

        Avaliacao aAvaliaB = Avaliacao.pendente(proposta, evento.usuarioA(), evento.usuarioB());
        Avaliacao bAvaliaA = Avaliacao.pendente(proposta, evento.usuarioB(), evento.usuarioA());

        avaliacaoRepository.saveAll(List.of(aAvaliaB, bAvaliaA));
    }
}
