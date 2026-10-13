package com.trokr.model.state.contraproposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * State Pattern — máquina da CONTRAPROPOSTA (propostaPai != null).
 *
 * Opção A da Aula 5-6: interface separada da raiz ({@code EstadoProposta}).
 * A mesma entidade Proposta usa esta interface quando é uma réplica, e a
 * outra quando é raiz — cada máquina 100% independente.
 */
public interface EstadoContraproposta {

    void irParaRascunho(Proposta contraproposta);

    void irParaEmAnalise(Proposta contraproposta);

    void irParaNegociado(Proposta contraproposta);

    void irParaFinalizado(Proposta contraproposta);

    void irParaRecusado(Proposta contraproposta);

    void irParaCancelado(Proposta contraproposta);

    /**
     * Efeito cascata: chamado em todas as contrapropostas de uma raiz quando
     * ela é encerrada (outra contraproposta fechou a troca, ou a raiz foi
     * cancelada). Cada estado decide o que isso significa para ele —
     * Rascunho/Em Análise viram Cancelado, Negociado vira Recusado e os
     * terminais não mudam. Nenhum if de status fora do padrão.
     */
    void encerrarPorFimDaRaiz(Proposta contraproposta);

    /** false nos estados terminais (Finalizado, Recusado, Cancelado). */
    boolean isPodeMudar();

    /** Valor do enum persistido no banco para este estado. */
    Status getStatus();
}
