package com.trokr.notificacao;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** O adaptador da Aula 13 (antes NotificacaoService): sorteio e interruptor previsíveis. */
class NotificacaoSimuladaAdapterTest {

    private static NotificacaoProperties props(double taxaFalha) {
        return new NotificacaoProperties(taxaFalha, 0, 3, 1, 5, Duration.ofSeconds(30));
    }

    private final SimulacaoNotificacao simulacao = new SimulacaoNotificacao();

    @Test
    void entregaQuandoOSorteioFicaAcimaDaTaxa() {
        new NotificacaoSimuladaAdapter(props(0.3), simulacao, () -> 0.9).notificar(1L, "oi");
    }

    @Test
    void falhaQuandoOSorteioFicaAbaixoDaTaxa() {
        NotificacaoSimuladaAdapter adaptador = new NotificacaoSimuladaAdapter(props(0.3), simulacao, () -> 0.1);
        assertThrows(NotificacaoIndisponivelException.class, () -> adaptador.notificar(1L, "oi"));
    }

    @Test
    void foraDoArTodaChamadaFalhaMesmoComTaxaZero() {
        NotificacaoSimuladaAdapter adaptador = new NotificacaoSimuladaAdapter(props(0.0), simulacao, () -> 0.9);
        simulacao.derrubar();
        assertThrows(NotificacaoIndisponivelException.class, () -> adaptador.notificar(1L, "oi"));

        simulacao.religar();
        adaptador.notificar(1L, "oi"); // de volta: não lança
    }

    @Test
    void taxaForaDoIntervaloEDefeitoDeConfiguracao() {
        assertThrows(IllegalArgumentException.class, () -> props(1.5));
    }
}
