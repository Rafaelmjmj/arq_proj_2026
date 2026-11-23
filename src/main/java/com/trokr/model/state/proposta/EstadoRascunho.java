package com.trokr.model.state.proposta;

import com.trokr.exception.TransicaoInvalidaException;
import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Ponto de partida da Proposta raiz. Pode ser submetida para homologação ou cancelada.
 */
public class EstadoRascunho implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Rascunho → ";

    @Override
    public void irParaRascunho(Proposta proposta) {
        throw TransicaoInvalidaException.daProposta("A proposta já está como Rascunho. Transição inválida!");
    }

    @Override
    public void irParaHomologacao(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoHomologacao());
    }

    @Override
    public void irParaAtiva(Proposta proposta) {
        throw TransicaoInvalidaException.daProposta(TRANSICAO_INVALIDA + "Ativa");
    }

    @Override
    public void irParaNegociado(Proposta proposta) {
        throw TransicaoInvalidaException.daProposta(TRANSICAO_INVALIDA + "Negociado");
    }

    @Override
    public void irParaFinalizado(Proposta proposta) {
        throw TransicaoInvalidaException.daProposta(TRANSICAO_INVALIDA + "Finalizado");
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
        return Status.RASCUNHO;
    }
}
