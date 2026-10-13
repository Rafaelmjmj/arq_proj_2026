package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Aguardando aprovação da plataforma. Pode ser aprovada (Ativa), voltar para edição (Rascunho) ou ser cancelada.
 */
public class EstadoHomologacao implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Homologação → ";

    @Override
    public void irParaRascunho(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoRascunho());
    }

    @Override
    public void irParaHomologacao(Proposta proposta) {
        throw new IllegalStateException("A proposta já está como Homologação. Transição inválida!");
    }

    @Override
    public void irParaAtiva(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoAtiva());
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
        proposta.mudarEstadoPara(new EstadoCancelado());
    }

    @Override
    public boolean isPodeMudar() {
        return true;
    }

    @Override
    public Status getStatus() {
        return Status.HOMOLOGACAO;
    }
}
