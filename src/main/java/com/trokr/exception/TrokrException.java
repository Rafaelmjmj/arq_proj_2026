package com.trokr.exception;

/**
 * Base das exceções de DOMÍNIO do Trokr (Aula 11): a regra de negócio foi
 * aplicada e recusou o pedido. É esperada, tem significado de negócio e
 * quem chamou pode reagir — vira 4xx.
 *
 * <p>Cada ocorrência carrega um {@code codigo} estável (ex.:
 * PROPOSTA_TRANSICAO_INVALIDA), que o cliente da API compara para decidir o
 * que fazer. A mensagem pode mudar; o código, uma vez publicado, não.
 *
 * <p>Decisões de projeto:
 * <ul>
 *   <li><b>Unchecked:</b> o @Transactional só faz rollback de unchecked por
 *       padrão; uma recusa no meio da transação desfaz tudo.</li>
 *   <li><b>Opção B — a exceção não conhece HTTP:</b> ela diz O QUE aconteceu;
 *       o GlobalExceptionHandler decide o status. O domínio continua sem
 *       dependência da camada web (na Aula 12 estas exceções também aparecem
 *       num agendamento, sem requisição nenhuma).</li>
 *   <li><b>sealed:</b> o compilador conhece todas as subclasses, e o switch
 *       do handler deixa de compilar se uma nova ficar de fora.</li>
 * </ul>
 * Falhas técnicas (Mongo fora do ar, NullPointerException, notificação
 * indisponível) NÃO entram nesta hierarquia: elas viram 500 ou são tratadas
 * onde acontecem.
 */
public abstract sealed class TrokrException extends RuntimeException
        permits RecursoNaoEncontradoException, TransicaoInvalidaException,
                RegraNegocioException, AcessoNegadoException {

    private final String codigo;

    protected TrokrException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
