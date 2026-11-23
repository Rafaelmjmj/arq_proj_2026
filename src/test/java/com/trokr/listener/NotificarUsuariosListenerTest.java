package com.trokr.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Fixtures;
import com.trokr.model.Usuario;
import com.trokr.notificacao.NotificacaoIndisponivelException;
import com.trokr.notificacao.NotificacaoPort;
import com.trokr.notificacao.PortaDeTeste;
import com.trokr.notificacao.RegistroEmMemoria;
import com.trokr.service.LogEventoService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Aula 13 — o listener só com a regra: conhece a NotificacaoPort, avisa as
 * duas pessoas, e registra a falha definitiva de cada uma sem deixar nada
 * subir. O retry não está mais aqui (NotificacaoComRetryTest).
 */
class NotificarUsuariosListenerTest {

    private final RegistroEmMemoria registro = new RegistroEmMemoria();
    private final Usuario ana = Fixtures.usuario(1, "Ana");
    private final Usuario bruno = Fixtures.usuario(2, "Bruno");
    private final TrocaConcluidaEvent evento = new TrocaConcluidaEvent(10L, ana, bruno,
            Fixtures.item(1, "Bicicleta", CategoriaItem.PRODUTO, ana),
            Fixtures.item(2, "Aula de violão", CategoriaItem.SERVICO, bruno),
            LocalDateTime.now());

    private NotificarUsuariosListener listener(NotificacaoPort porta) {
        return new NotificarUsuariosListener(porta, new LogEventoService(registro));
    }

    @Test
    void avisaAsDuasPessoasPelaPorta() {
        PortaDeTeste porta = new PortaDeTeste(0);
        listener(porta).aoConcluirTroca(evento);

        assertEquals(List.of(1L, 2L), porta.entregues);
        assertTrue(registro.eventos.isEmpty());
    }

    @Test
    void falhaDeUmaPessoaNaoImpedeAOutra() {
        PortaDeTeste porta = new PortaDeTeste(1); // só a 1ª chamada (Ana) falha
        listener(porta).aoConcluirTroca(evento);   // não lança

        assertEquals(List.of(2L), porta.entregues);
        assertEquals(1, registro.buscarPorTipo(LogEventoService.TIPO_ERRO_SISTEMA).size());
    }

    @Test
    void comAPilhaDesistindoRegistraUmaFalhaPorPessoa() {
        NotificacaoPort semServico = (id, msg) -> {
            throw new NotificacaoIndisponivelException("fora do ar");
        };
        listener(semServico).aoConcluirTroca(evento);

        assertEquals(2, registro.buscarPorTipo(LogEventoService.TIPO_ERRO_SISTEMA).size());
    }
}
