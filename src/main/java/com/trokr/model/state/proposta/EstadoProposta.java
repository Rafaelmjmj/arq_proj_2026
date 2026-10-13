package com.trokr.model.state.proposta;

import com.trokr.model.Proposta;
import com.trokr.model.Status;

/**
 * State Pattern — máquina da Proposta RAIZ (propostaPai == null).
 *
 * Decisão da turma: Opção A (Aula 5-6, Deck 4) — duas interfaces separadas.
 * Esta aqui só conhece os 6 estados da raiz; a contraproposta tem a sua
 * própria interface ({@code EstadoContraproposta}), sem nenhum if do tipo
 * "se for contraproposta..." escondido dentro dos estados.
 *
 * Cada estado concreto sabe as SUAS transições válidas e lança
 * IllegalStateException para todas as outras.
 */
public interface EstadoProposta {

    void irParaRascunho(Proposta proposta);

    void irParaHomologacao(Proposta proposta);

    void irParaAtiva(Proposta proposta);

    void irParaNegociado(Proposta proposta);

    void irParaFinalizado(Proposta proposta);

    void irParaCancelado(Proposta proposta);

    /** false nos estados terminais (Finalizado, Cancelado). */
    boolean isPodeMudar();

    /** Valor do enum persistido no banco para este estado. */
    Status getStatus();

    /**
     * Só uma proposta Ativa aceita novas contrapropostas. O padrão é false;
     * apenas EstadoAtiva sobrescreve — de novo, cada estado decide por si.
     */
    default boolean podeReceberContraproposta() {
        return false;
    }
}
