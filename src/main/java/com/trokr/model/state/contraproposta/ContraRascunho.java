package com.trokr.model.state.contraproposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Ponto de partida da contraproposta — ainda não foi enviada ao dono da Proposta original.
 * Pode ser submetida (Em Análise) ou cancelada pelo próprio autor.
 */
public class ContraRascunho implements EstadoContraproposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida (contraproposta): Rascunho → ";

    @Override
    public void irParaRascunho(Proposta contraproposta) {
        throw new IllegalStateException("A contraproposta já está como Rascunho. Transição inválida!");
    }

    @Override
    public void irParaEmAnalise(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraAnalise());
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
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Recusado");
    }

    @Override
    public void irParaCancelado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraCancelado());
    }

    /** Efeito cascata. Ainda não tinha sido enviada: vira Cancelado. */
    @Override
    public void encerrarPorFimDaRaiz(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraCancelado());
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
