package com.trokr.exception;

/**
 * Alguém tenta agir em nome de outra pessoa → 403. Ex.: aceitar uma
 * contraproposta sem ser o dono da proposta original
 * (PROPOSTA_ACAO_NAO_AUTORIZADA), preencher a avaliação de outro
 * (AVALIACAO_AUTOR_INVALIDO).
 *
 * Fica como uma quarta subclasse (e não como um código de
 * RegraNegocioException) porque a reação esperada do cliente é diferente:
 * não há dado para corrigir, a pessoa simplesmente não pode fazer isso.
 *
 * Ainda não existe autenticação: o "usuário que está chamando" vem como
 * parâmetro (usuarioId) na requisição.
 */
public final class AcessoNegadoException extends TrokrException {

    public AcessoNegadoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
