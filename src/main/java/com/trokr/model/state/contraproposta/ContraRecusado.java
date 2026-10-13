package com.trokr.model.state.contraproposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Terminal — o dono original recusou, ou uma das partes desistiu durante a negociação.
 */
public class ContraRecusado implements EstadoContraproposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida (contraproposta): Recusado → ";

    @Override
    public void irParaRascunho(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Rascunho");
    }

    @Override
    public void irParaEmAnalise(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Em Análise");
    }

    @Override
    public void irParaNegociado(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Negociado");
    }

    @Override
    public void irParaFinalizado(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Finalizado");
    }

    @Override
    public void irParaRecusado(Proposta contraproposta) {
        throw new IllegalStateException("A contraproposta já está como Recusado. Transição inválida!");
    }

    @Override
    public void irParaCancelado(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Cancelado");
    }

    /** Efeito cascata. Terminal: nada muda. */
    @Override
    public void encerrarPorFimDaRaiz(Proposta contraproposta) {
        // Terminal: já está encerrada, nada a fazer.
    }

    @Override
    public boolean isPodeMudar() {
        return false;
    }

    @Override
    public Status getStatus() {
        return Status.RECUSADO;
    }
}
