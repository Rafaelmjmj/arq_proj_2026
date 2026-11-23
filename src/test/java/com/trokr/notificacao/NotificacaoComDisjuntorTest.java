package com.trokr.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trokr.service.LogEventoService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** O disjuntor isolado: os três estados, com o relógio na mão do teste. */
class NotificacaoComDisjuntorTest {

    private static final Duration TRINTA_S = Duration.ofSeconds(30);

    private final RegistroEmMemoria registro = new RegistroEmMemoria();
    private final RelogioDeTeste relogio = new RelogioDeTeste();
    private final PortaDeTeste porta = new PortaDeTeste(Integer.MAX_VALUE);
    private final NotificacaoComDisjuntor disjuntor =
            new NotificacaoComDisjuntor(porta, 3, TRINTA_S, new LogEventoService(registro), relogio);

    private void falhar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertThrows(NotificacaoIndisponivelException.class, () -> disjuntor.notificar(1L, "oi"));
        }
    }

    private int pendentes() {
        return registro.buscarPorTipo(NotificacaoComDisjuntor.TIPO_PENDENTE).size();
    }

    @Test
    void fechadoDeixaPassarEContaAsFalhas() {
        falhar(2);
        assertEquals("FECHADO", disjuntor.estado());
        assertEquals(2, porta.chamadas);
    }

    @Test
    void abreNoLimiteEDaiEmDianteNadaPassa() {
        falhar(3);
        assertEquals("ABERTO", disjuntor.estado());

        disjuntor.notificar(2L, "oi"); // não lança: vira pendente
        assertEquals(3, porta.chamadas);
        assertEquals(1, pendentes());
    }

    @Test
    void umSucessoZeraOContador() {
        porta.falharAsProximas(2);
        falhar(2);
        disjuntor.notificar(1L, "oi");   // sucesso
        porta.falharAsProximas(2);
        falhar(2);
        assertEquals("FECHADO", disjuntor.estado());
    }

    @Test
    void meioAbertoComSucessoFecha() {
        falhar(3);
        relogio.avancar(TRINTA_S);
        assertEquals("MEIO_ABERTO", disjuntor.estado());

        porta.falharAsProximas(0);       // o serviço voltou
        disjuntor.notificar(1L, "oi");
        assertEquals("FECHADO", disjuntor.estado());
        assertEquals(1, porta.entregues.size());
    }

    @Test
    void meioAbertoComFalhaAbreDeNovoNaHora() {
        falhar(3);
        relogio.avancar(TRINTA_S);

        falhar(1);                       // o teste falhou: basta uma
        assertEquals("ABERTO", disjuntor.estado());
        disjuntor.notificar(1L, "oi");
        assertEquals(1, pendentes());
    }

    @Test
    void falhaAoRegistrarPendenciaNaoSobe() {
        LogEventoService mongoFora = new LogEventoService(registro) {
            @Override
            public void registrarAviso(String tipo, String origem, String mensagem, java.util.Map<String, Object> p) {
                throw new IllegalStateException("Mongo fora");
            }
        };
        NotificacaoComDisjuntor d = new NotificacaoComDisjuntor(porta, 1, TRINTA_S, mongoFora, relogio);
        assertThrows(NotificacaoIndisponivelException.class, () -> d.notificar(1L, "oi"));
        d.notificar(1L, "oi"); // aberto + registro quebrado: ainda assim não lança
    }
}
