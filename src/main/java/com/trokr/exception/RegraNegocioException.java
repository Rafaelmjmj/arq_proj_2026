package com.trokr.exception;

/**
 * Dado bem formado que viola uma regra de negócio → 422. Ex.: oferecer item
 * de outro usuário (ITEM_DE_OUTRO_USUARIO), contrapropor a própria proposta
 * (CONTRAPROPOSTA_PARA_SI_MESMO). As várias regras se diferenciam pelo código,
 * não por uma subclasse para cada uma.
 */
public final class RegraNegocioException extends TrokrException {

    public RegraNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
