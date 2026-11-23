package com.trokr.notificacao;

import com.trokr.config.AtrasoDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator 1 de 2 (Aula 13): tenta de novo. É uma NotificacaoPort que
 * envolve outra NotificacaoPort. O laço é o mesmo que, na Aula 11, morava no
 * NotificarUsuariosListener; virou camada, e o listener ficou só com a regra.
 *
 * <ul>
 *   <li>repete só NotificacaoIndisponivelException (falha transitória);</li>
 *   <li>até {@code maxTentativas}, com espera que dobra a cada falha
 *       (200, 400, 800 ms: backoff exponencial);</li>
 *   <li>esgotadas as tentativas, relança: quem decide o que fazer com a
 *       falha definitiva é o listener.</li>
 * </ul>
 *
 * Não é @Component: quem monta a pilha é a NotificacaoConfig.
 */
public class NotificacaoComRetry implements NotificacaoPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoComRetry.class);

    private final NotificacaoPort proximo;
    private final int maxTentativas;
    private final long esperaInicialMs;

    public NotificacaoComRetry(NotificacaoPort proximo, int maxTentativas, long esperaInicialMs) {
        this.proximo = proximo;
        this.maxTentativas = Math.max(1, maxTentativas);
        this.esperaInicialMs = esperaInicialMs;
    }

    @Override
    public void notificar(Long usuarioId, String mensagem) {
        long espera = esperaInicialMs;
        for (int tentativa = 1; ; tentativa++) {
            try {
                proximo.notificar(usuarioId, mensagem);
                return;
            } catch (NotificacaoIndisponivelException ex) {
                if (tentativa >= maxTentativas) {
                    throw ex;
                }
                log.warn("Tentativa {} de {} para o usuário {} falhou; nova tentativa em {} ms",
                        tentativa, maxTentativas, usuarioId, espera);
                AtrasoDemo.dormir(espera);
                espera *= 2;
            }
        }
    }
}
