package com.trokr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Componente utilitário exclusivo para demonstração didática de condições de corrida em aula.
 *
 * <p>Permite injetar pausas artificiais controladas (via propriedade {@code trokr.demo.atraso-ms}
 * ou variável {@code TROKR_DEMO_ATRASOMS}) para alargar janelas de concorrência e tornar
 * bugs como lost updates e inconsistências de estado reproduzíveis.</p>
 *
 * <p><b>Importante:</b> Em ambiente de produção o valor deve ser sempre 0 (zero),
 * não causando qualquer impacto no tempo de resposta da aplicação.</p>
 *
 * <p>Além do modelo da aula, este componente expõe {@link #dormir(long)}: a mesma pausa
 * que trata {@code InterruptedException}, reaproveitada pelo retry da notificação (Aula 11).</p>
 */
@Component
public class AtrasoDemo {

    private final long atrasoMs;

    public AtrasoDemo(@Value("${trokr.demo.atraso-ms:0}") long atrasoMs) {
        this.atrasoMs = atrasoMs;
    }

    /**
     * Aplica o atraso configurado se for maior que zero.
     * Caso contrário, retorna imediatamente.
     */
    public void aplicar() {
        if (atrasoMs <= 0) {
            return;
        }
        dormir(atrasoMs);
    }

    public long getAtrasoMs() {
        return atrasoMs;
    }

    /**
     * Thread.sleep que trata InterruptedException: devolve o sinal de
     * interrupção à thread em vez de engoli-lo.
     */
    public static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
