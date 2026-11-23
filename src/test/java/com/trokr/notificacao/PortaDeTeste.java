package com.trokr.notificacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma NotificacaoPort falsa para testar as camadas isoladas: falha nas
 * primeiras {@code falhas} chamadas (ou sempre, com Integer.MAX_VALUE) e
 * depois entrega. Também é um "adaptador" da porta, só que de teste.
 */
public class PortaDeTeste implements NotificacaoPort {

    private int falhas;
    public int chamadas = 0;
    public final List<Long> entregues = new ArrayList<>();

    public PortaDeTeste(int falhas) {
        this.falhas = falhas;
    }

    /** Muda o comportamento no meio do teste: 0 = serviço de volta. */
    public void falharAsProximas(int falhas) {
        this.falhas = chamadas + falhas;
    }

    @Override
    public void notificar(Long usuarioId, String mensagem) {
        chamadas++;
        if (chamadas <= falhas) {
            throw new NotificacaoIndisponivelException("simulada");
        }
        entregues.add(usuarioId);
    }
}
