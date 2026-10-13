package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Terminal — a troca aconteceu de fato. É o gatilho do TrocaConcluidaEvent (Observer),
 * publicado pelo PropostaService depois da transição.
 */
public class EstadoFinalizado implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Finalizado → ";

    @Override
    public void irParaRascunho(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Rascunho");
    }

    @Override
    public void irParaHomologacao(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Homologação");
    }

    @Override
    public void irParaAtiva(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Ativa");
    }

    @Override
    public void irParaNegociado(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Negociado");
    }

    @Override
    public void irParaFinalizado(Proposta proposta) {
        throw new IllegalStateException("A proposta já está como Finalizado. Transição inválida!");
    }

    @Override
    public void irParaCancelado(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Cancelado");
    }

    @Override
    public boolean isPodeMudar() {
        return false;
    }

    @Override
    public Status getStatus() {
        return Status.FINALIZADO;
    }
}
