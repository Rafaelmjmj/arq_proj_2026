package com.trokr.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trokr.service.LogEventoService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * A pilha montada como na NotificacaoConfig (retry → disjuntor → porta),
 * reproduzindo o "Conferir" do passo 8 da aula: serviço fora do ar,
 * limite 5, 3 tentativas, duas trocas (pessoas A e B em cada).
 */
class PilhaDeNotificacaoTest {

    private final RegistroEmMemoria registro = new RegistroEmMemoria();
    private final PortaDeTeste servicoFora = new PortaDeTeste(Integer.MAX_VALUE);
    private final NotificacaoComDisjuntor disjuntor = new NotificacaoComDisjuntor(
            servicoFora, 5, Duration.ofSeconds(30), new LogEventoService(registro), new RelogioDeTeste());
    private final NotificacaoPort pilha = new NotificacaoComRetry(disjuntor, 3, 1);

    @Test
    void reproduzOPasso8DaAula() {
        // 1ª troca, pessoa A: tentativas 1, 2 e 3 falham (contador 3) → falha definitiva
        assertThrows(NotificacaoIndisponivelException.class, () -> pilha.notificar(1L, "A"));
        assertEquals(3, servicoFora.chamadas);

        // 1ª troca, pessoa B: falha (4), falha (5) → ABERTO; tentativa 3 → pendente, sem exceção
        pilha.notificar(2L, "B");
        assertEquals(5, servicoFora.chamadas);
        assertEquals("ABERTO", disjuntor.estado());

        // 2ª troca, A e B: nenhuma chamada ao serviço, as duas pendentes na hora
        pilha.notificar(1L, "A");
        pilha.notificar(2L, "B");
        assertEquals(5, servicoFora.chamadas);

        assertEquals(3, registro.buscarPorTipo(NotificacaoComDisjuntor.TIPO_PENDENTE).size());
    }
}
