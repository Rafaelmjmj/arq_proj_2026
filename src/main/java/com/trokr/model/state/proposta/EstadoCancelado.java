package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Terminal — o autor desistiu da proposta. Nenhuma transição sai daqui.
 */
public class EstadoCancelado implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Cancelado → ";

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
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Finalizado");
    }

    @Override
    public void irParaCancelado(Proposta proposta) {
        throw new IllegalStateException("A proposta já está como Cancelado. Transição inválida!");
    }

    @Override
    public boolean isPodeMudar() {
        return false;
    }

    @Override
    public Status getStatus() {
        return Status.CANCELADO_RAIZ;
    }
}
