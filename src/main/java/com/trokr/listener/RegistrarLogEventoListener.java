package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.service.LogEventoService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener 4 de 5 (Aula 8) — grava um LogEvento TROCA_CONCLUIDA no MongoDB.
 *
 * Só conhece o LogEventoService, que por sua vez só conhece a porta
 * RegistroDeEventos (Adapter). Este listener nem sabe que existe Mongo.
 *
 * Aula 10, Corrida 3 — AFTER_COMMIT: o Mongo não participa da transação JPA
 * do Postgres. Se este log fosse gravado na hora em que o evento é
 * publicado, a finalização que perde a corrida (rollback pelo @Version)
 * deixaria no Mongo o log de uma troca que, para o banco, nunca aconteceu.
 * Agora o Spring guarda o evento e só chama este método depois que o commit
 * deu certo; em rollback, ele simplesmente não roda.
 *
 * Depois do commit não há como desfazer a troca: se o Mongo estiver fora do
 * ar, o log se perde (o LogEventoService registra a falha no console) e a
 * troca continua concluída. Um log a menos é melhor que um log falso.
 */
@Component
@RequiredArgsConstructor
public class RegistrarLogEventoListener {

    public static final String TIPO = "TROCA_CONCLUIDA";

    private final LogEventoService logEventoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propostaId", evento.propostaId());
        payload.put("usuarioAId", evento.usuarioA().getId());
        payload.put("usuarioBId", evento.usuarioB().getId());
        payload.put("itemAId", evento.itemA().getId());
        payload.put("itemBId", evento.itemB().getId());
        payload.put("dataConclusao", evento.dataConclusao().toString());

        logEventoService.registrarInfo(TIPO, "PropostaService", "Troca concluída", payload, null);
    }
}
