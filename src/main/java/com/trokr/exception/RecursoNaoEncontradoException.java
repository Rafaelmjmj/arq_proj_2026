package com.trokr.exception;

/**
 * O recurso pedido não existe → 404. Substitui a antiga
 * ResourceNotFoundException. Códigos: USUARIO_NAO_ENCONTRADO,
 * ITEM_NAO_ENCONTRADO, PROPOSTA_NAO_ENCONTRADA, AVALIACAO_NAO_ENCONTRADA.
 */
public final class RecursoNaoEncontradoException extends TrokrException {

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public static RecursoNaoEncontradoException usuario(Long id) {
        return new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado com id " + id);
    }

    public static RecursoNaoEncontradoException item(Long id) {
        return new RecursoNaoEncontradoException("ITEM_NAO_ENCONTRADO", "Item não encontrado com id " + id);
    }

    public static RecursoNaoEncontradoException proposta(Long id) {
        return new RecursoNaoEncontradoException("PROPOSTA_NAO_ENCONTRADA", "Proposta não encontrada com id " + id);
    }

    public static RecursoNaoEncontradoException avaliacao(Long id) {
        return new RecursoNaoEncontradoException("AVALIACAO_NAO_ENCONTRADA", "Avaliação não encontrada com id " + id);
    }
}
