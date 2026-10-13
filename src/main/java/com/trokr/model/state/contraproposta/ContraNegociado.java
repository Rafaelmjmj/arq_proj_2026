package com.trokr.model.state.contraproposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Aceita pelo dono original. Aguardando a negociação presencial se concretizar.
 * Termina em Finalizado (troca concluída) ou Recusado (uma das partes desistiu).
 */
public class ContraNegociado implements EstadoContraproposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida (contraproposta): Negociado → ";

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
        throw new IllegalStateException("A contraproposta já está como Negociado. Transição inválida!");
    }

    @Override
    public void irParaFinalizado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraFinalizado());
    }

    @Override
    public void irParaRecusado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraRecusado());
    }

    @Override
    public void irParaCancelado(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Cancelado");
    }

    /** Efeito cascata. Estava em negociação e a raiz foi cancelada: a negociação não se concretizou, vira Recusado. */
    @Override
    public void encerrarPorFimDaRaiz(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraRecusado());
    }

    @Override
    public boolean isPodeMudar() {
        return true;
    }

    @Override
    public Status getStatus() {
        return Status.NEGOCIADO_CONTRA;
    }
}
