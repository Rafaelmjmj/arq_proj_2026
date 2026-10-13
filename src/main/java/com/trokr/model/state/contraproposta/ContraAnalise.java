package com.trokr.model.state.contraproposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * Enviada. Aguardando a decisão do dono da Proposta original: aceitar (Negociado) ou
 * recusar (Recusado). O autor ainda pode voltar para Rascunho para editar, ou cancelar.
 */
public class ContraAnalise implements EstadoContraproposta {

    private static final String TRANSICAO_INVALIDA = "Transição inválida (contraproposta): Em Análise → ";

    @Override
    public void irParaRascunho(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraRascunho());
    }

    @Override
    public void irParaEmAnalise(Proposta contraproposta) {
        throw new IllegalStateException("A contraproposta já está como Em Análise. Transição inválida!");
    }

    @Override
    public void irParaNegociado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraNegociado());
    }

    @Override
    public void irParaFinalizado(Proposta contraproposta) {
        throw new IllegalStateException(TRANSICAO_INVALIDA + "Finalizado");
    }

    @Override
    public void irParaRecusado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraRecusado());
    }

    @Override
    public void irParaCancelado(Proposta contraproposta) {
        contraproposta.mudarEstadoPara(new ContraCancelado());
    }

    /** Efeito cascata. Estava concorrendo e outra fechou a troca (ou a raiz foi cancelada): vira Cancelado. */
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
        return Status.EM_ANALISE;
    }
}
