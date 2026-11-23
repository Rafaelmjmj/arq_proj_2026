package com.trokr.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** O Decorator de retry isolado, sobre uma porta falsa. Espera de 1 ms. */
class NotificacaoComRetryTest {

    @Test
    void entregaNaTerceiraTentativa() {
        PortaDeTeste porta = new PortaDeTeste(2);
        new NotificacaoComRetry(porta, 3, 1).notificar(1L, "oi");

        assertEquals(3, porta.chamadas);
        assertEquals(1, porta.entregues.size());
    }

    @Test
    void esgotadasAsTentativasRelancaParaQuemChamou() {
        PortaDeTeste porta = new PortaDeTeste(Integer.MAX_VALUE);
        NotificacaoComRetry retry = new NotificacaoComRetry(porta, 3, 1);

        assertThrows(NotificacaoIndisponivelException.class, () -> retry.notificar(1L, "oi"));
        assertEquals(3, porta.chamadas);
    }

    @Test
    void naoRepeteFalhaQueNaoEhTransitoria() {
        int[] chamadas = {0};
        NotificacaoPort quebrada = (id, msg) -> {
            chamadas[0]++;
            throw new IllegalArgumentException("usuário sem contato");
        };
        assertThrows(IllegalArgumentException.class, () -> new NotificacaoComRetry(quebrada, 3, 1).notificar(1L, "oi"));
        assertEquals(1, chamadas[0]);
    }
}
