package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.Item;
import com.trokr.model.Usuario;
import com.trokr.notificacao.NotificacaoPort;
import com.trokr.service.LogEventoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener 3 de 5 — notifica os dois usuários de que a troca foi concluída.
 *
 * <p>Aula 10, Corrida 3 — AFTER_COMMIT: uma mensagem enviada não tem como ser
 * "desenviada", então o envio só acontece quando não há mais rollback
 * possível.
 *
 * <p>Aula 13 — o listener fica só com a REGRA: para quem mandar, o que
 * mandar e o que fazer se a pilha desistir. Ele conhece apenas a
 * {@link NotificacaoPort}; o retry (Aula 11) saiu daqui e virou a camada
 * NotificacaoComRetry, e o disjuntor entrou por dentro dele (NotificacaoConfig).
 *
 * <p>Notificação é dependência ACESSÓRIA: a troca vale sem ela. Por isso
 * nada sobe daqui. Cada pessoa é tentada separadamente: se a primeira
 * falhar de vez, a segunda ainda é tentada.
 */
@Component
public class NotificarUsuariosListener {

    private static final Logger log = LoggerFactory.getLogger(NotificarUsuariosListener.class);

    private final NotificacaoPort notificacaoPort;
    private final LogEventoService logEventoService;

    public NotificarUsuariosListener(NotificacaoPort notificacaoPort, LogEventoService logEventoService) {
        this.notificacaoPort = notificacaoPort;
        this.logEventoService = logEventoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        notificar(evento, evento.usuarioA(), evento.itemA(), evento.itemB(), evento.usuarioB());
        notificar(evento, evento.usuarioB(), evento.itemB(), evento.itemA(), evento.usuarioA());
    }

    private void notificar(TrocaConcluidaEvent evento, Usuario destinatario, Item itemEntregue,
                           Item itemRecebido, Usuario outraParte) {
        Long usuarioId = destinatario == null ? null : destinatario.getId();
        try {
            String mensagem = "Olá, " + destinatario.getNome() + "! Sua troca da proposta " + evento.propostaId()
                    + " foi concluída: você entregou \"" + itemEntregue.getTitulo() + "\" e recebeu \""
                    + itemRecebido.getTitulo()
                    + "\" de " + outraParte.getNome() + ". Não esqueça de avaliar a troca.";
            notificacaoPort.notificar(usuarioId, mensagem);
        } catch (Exception ex) {
            log.error("Notificação para o usuário {} falhou", usuarioId, ex);
            registrarFalha(evento, usuarioId, ex);
        }
    }

    /** O registro também pode falhar (Mongo fora): nada sobe daqui. */
    private void registrarFalha(TrocaConcluidaEvent evento, Long usuarioId, Exception ex) {
        try {
            logEventoService.registrarErro("NotificarUsuariosListener",
                    "Notificação da troca " + evento.propostaId() + " para o usuário " + usuarioId
                            + " não entregue", ex);
        } catch (Exception falhaNoRegistro) {
            log.error("Não foi possível registrar a falha de notificação", falhaNoRegistro);
        }
    }
}
