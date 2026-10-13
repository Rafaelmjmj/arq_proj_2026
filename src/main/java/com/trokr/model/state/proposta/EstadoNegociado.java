package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Uma contraproposta foi aceita e a negociação presencial está em andamento.
 * Pode ser finalizada (troca realizada), voltar para Ativa (negociação falhou) ou ser cancelada.
 */
public class EstadoNegociado implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Negociado → ";

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
        proposta.mudarEstadoPara(new EstadoAtiva());
    }

    @Override
    public void irParaNegociado(Proposta proposta) {
        throw new IllegalStateException("A proposta já está como Negociado. Transição inválida!");
    }

    @Override
    public void irParaFinalizado(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoFinalizado());
    }

    @Override
    public void irParaCancelado(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoCancelado());
    }

    @Override
    public boolean isPodeMudar() {
        return true;
    }

    @Override
    public Status getStatus() {
        return Status.NEGOCIADO_RAIZ;
    }
}
