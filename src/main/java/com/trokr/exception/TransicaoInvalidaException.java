package com.trokr.exception;

/**
 * O estado atual não permite a ação pedida → 409 (conflito com o estado do
 * recurso). Lançada pelas classes de estado (State) e pelas checagens de
 * estado do domínio. Só o domínio do Trokr lança esta exceção, sempre por
 * regra de negócio — diferente de IllegalStateException, que também vem de
 * bibliotecas e de bugs e por isso cai no 500.
 */
public final class TransicaoInvalidaException extends TrokrException {

    public static final String PROPOSTA = "PROPOSTA_TRANSICAO_INVALIDA";
    public static final String CONTRAPROPOSTA = "CONTRAPROPOSTA_TRANSICAO_INVALIDA";

    public TransicaoInvalidaException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    /** Transição recusada pela máquina da Proposta raiz. */
    public static TransicaoInvalidaException daProposta(String mensagem) {
        return new TransicaoInvalidaException(PROPOSTA, mensagem);
    }

    /** Transição recusada pela máquina da Contraproposta. */
    public static TransicaoInvalidaException daContraproposta(String mensagem) {
        return new TransicaoInvalidaException(CONTRAPROPOSTA, mensagem);
    }
}
