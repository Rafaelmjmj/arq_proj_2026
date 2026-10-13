package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 3 de 5 — "notifica" os dois usuários.
 *
 * Simulado: ainda não existe serviço real de push/e-mail, então a
 * notificação é um log estruturado no console. A estrutura de listener já é
 * a definitiva — trocar o log por um envio real não mexe em mais nada.
 */
@Component
public class NotificarUsuariosListener {

    private static final Logger log = LoggerFactory.getLogger(NotificarUsuariosListener.class);

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        notificar(evento.usuarioA().getEmail(), evento.usuarioA().getNome(),
                evento.itemA().getTitulo(), evento.itemB().getTitulo(), evento.usuarioB().getNome());
        notificar(evento.usuarioB().getEmail(), evento.usuarioB().getNome(),
                evento.itemB().getTitulo(), evento.itemA().getTitulo(), evento.usuarioA().getNome());
    }

    private void notificar(String email, String nome, String itemEntregue, String itemRecebido, String outraParte) {
        log.info("[NOTIFICACAO SIMULADA] para={} | Olá, {}! Sua troca foi concluída: você entregou \"{}\" "
                        + "e recebeu \"{}\" de {}. Não esqueça de avaliar a troca.",
                email, nome, itemEntregue, itemRecebido, outraParte);
    }
}
