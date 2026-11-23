package com.trokr.notificacao;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * As chaves {@code trokr.notificacao.*} do application.yml num record só
 * (Aula 13). Antes cada classe lia a sua com @Value; agora o adaptador e a
 * NotificacaoConfig recebem este objeto.
 *
 * @param taxaFalha       probabilidade de cada envio falhar no sorteio (0.0 a 1.0);
 *                        0.0 desde a Aula 13, porque quem derruba o serviço é o interruptor
 * @param latenciaMs      quanto cada envio demora
 * @param maxTentativas   tentativas do NotificacaoComRetry
 * @param esperaInicialMs espera antes da 2ª tentativa; dobra a cada falha
 * @param limiteFalhas    falhas seguidas que abrem o disjuntor
 * @param tempoAberto     quanto o disjuntor fica aberto antes da chamada de teste
 */
@ConfigurationProperties(prefix = "trokr.notificacao")
public record NotificacaoProperties(
        double taxaFalha,
        long latenciaMs,
        int maxTentativas,
        long esperaInicialMs,
        int limiteFalhas,
        Duration tempoAberto
) {

    public NotificacaoProperties {
        if (taxaFalha < 0.0 || taxaFalha > 1.0) {
            throw new IllegalArgumentException("trokr.notificacao.taxa-falha deve estar entre 0.0 e 1.0: " + taxaFalha);
        }
        maxTentativas = Math.max(1, maxTentativas);
        limiteFalhas = Math.max(1, limiteFalhas);
        if (tempoAberto == null) {
            tempoAberto = Duration.ofSeconds(30);
        }
    }
}
