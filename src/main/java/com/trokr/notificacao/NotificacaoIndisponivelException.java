package com.trokr.notificacao;

/**
 * O serviço de notificação não respondeu (Aula 11). Exceção TÉCNICA, não de
 * domínio: nenhuma regra de negócio recusou nada, uma dependência é que
 * falhou. Por isso NÃO entra na hierarquia TrokrException.
 *
 * <p>Aula 13: faz parte do CONTRATO da {@link NotificacaoPort} (por isso
 * mora no mesmo pacote). É também o que marca a falha como TRANSITÓRIA: o
 * {@link NotificacaoComRetry} repete e o {@link NotificacaoComDisjuntor}
 * conta só esta exceção; qualquer outra passa direto.
 */
public class NotificacaoIndisponivelException extends RuntimeException {

    public NotificacaoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
