package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Publicada e recebendo contrapropostas. Vai para Negociado quando o dono aceita uma contraproposta.
 * Também pode voltar para Rascunho (editar) ou ser cancelada.
 */
public class EstadoAtiva implements EstadoProposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida: Ativa → ";

    @Override
    public void irParaRascunho(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoRascunho());
    }

    @Override
    public void irParaHomologacao(Proposta proposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Homologação");
    }

    @Override
    public void irParaAtiva(Proposta proposta) {
        throw new IllegalStateException("A proposta já está como Ativa. Transição inválida!");
    }

    @Override
    public void irParaNegociado(Proposta proposta) {
        proposta.mudarEstadoPara(new EstadoNegociado());
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
    public boolean podeReceberContraproposta() {
        return true;
    }

    @Override
    public Status getStatus() {
        return Status.ATIVA;
    }
}
