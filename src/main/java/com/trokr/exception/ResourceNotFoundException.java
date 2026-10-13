package com.trokr.exception;

/**
 * Lançada quando um recurso buscado por id não existe.
 * Só temos esse tipo de exceção por enquanto — um tratamento de erros mais
 * robusto e padronizado (hierarquia de exceções, corpo de erro padronizado
 * etc.) é conteúdo de uma aula futura.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
