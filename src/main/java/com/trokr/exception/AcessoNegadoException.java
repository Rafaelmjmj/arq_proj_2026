package com.trokr.exception;

/**
 * Lançada quando alguém tenta agir em nome de outra pessoa — por exemplo,
 * aceitar uma contraproposta sem ser o dono da Proposta original, ou
 * preencher uma Avaliacao da qual não é o autor. Vira HTTP 403.
 *
 * Ainda não existe autenticação: o "usuário que está chamando" vem como
 * parâmetro (usuarioId) na requisição. Autenticação real é aula futura.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String message) {
        super(message);
    }
}
