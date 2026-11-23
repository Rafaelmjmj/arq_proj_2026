package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.service.LogEventoService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bônus da Aula 10 — AFTER_ROLLBACK: o contrário do AFTER_COMMIT. Só roda se
 * a transação que publicou o TrocaConcluidaEvent foi DESFEITA — tipicamente a
 * segunda finalização concorrente da mesma troca, recusada pelo @Version.
 *
 * Registra no LogEvento (WARN) que uma finalização foi tentada e não deu
 * certo. A troca em si continua íntegra: quem ganhou a corrida já gravou.
 *
 * Também mostra o Observer pagando dividendos: um listener a mais, e nenhuma
 * linha do PropostaService, do evento ou dos outros listeners mudou.
 */
@Component
@RequiredArgsConstructor
public class RegistrarFinalizacaoRecusadaListener {

    public static final String TIPO = "FINALIZACAO_RECUSADA";

    private final LogEventoService logEventoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void aoDesfazerTroca(TrocaConcluidaEvent evento) {
        logEventoService.registrarAviso(TIPO, "PropostaService",
                "Finalização desfeita (provável finalização concorrente da mesma troca)",
                Map.of("propostaId", evento.propostaId()));
    }
}
