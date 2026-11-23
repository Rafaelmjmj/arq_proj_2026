package com.trokr.notificacao;

/**
 * Porta de saída (Arquitetura Hexagonal, Aula 13): o que o domínio precisa
 * para avisar um usuário, em palavras do domínio. Pode lançar
 * {@link NotificacaoIndisponivelException}.
 *
 * <p>Um método só, de propósito: o adaptador e as duas camadas Decorator
 * (retry e disjuntor) implementam esta mesma interface, e cada método a mais
 * seria um método a mais em cada uma delas.
 *
 * <p>Mesma ideia da Aula 8: RegistroDeEventos é a porta do log, e o Mongo é
 * o adaptador.
 */
public interface NotificacaoPort {

    void notificar(Long usuarioId, String mensagem);
}
