package com.trokr.notificacao;

import com.trokr.service.LogEventoService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator 2 de 2 (Aula 13): o disjuntor (circuit breaker). Depois de
 * falhar muito, para de tentar.
 *
 * <pre>
 *   FECHADO ──(limiteFalhas falhas seguidas)──▶ ABERTO
 *   ABERTO  ──(passou tempoAberto)───────────▶ MEIO-ABERTO: uma chamada de teste
 *   MEIO-ABERTO ── teste funcionou ──▶ FECHADO
 *   MEIO-ABERTO ── teste falhou ─────▶ ABERTO de novo (mais tempoAberto)
 * </pre>
 *
 * <p>Aberto, nada passa: a notificação fica registrada como pendente
 * (LogEvento NOTIFICACAO_PENDENTE) e o método RETORNA NORMALMENTE — o retry,
 * por fora, entende como sucesso e para de tentar. É assim que a finalização
 * volta a ser rápida com o serviço morto.
 *
 * <p>Fica POR DENTRO do retry (NotificacaoConfig): cada tentativa do retry
 * passa pelo disjuntor e é contada. Por fora, o disjuntor só veria a falha
 * final de cada pessoa, depois de todas as esperas.
 *
 * <p>{@code synchronized}: o estado (contador e abertoAte) é compartilhado
 * por todas as requisições. Simplificação de aula: a chamada ao serviço
 * também fica dentro do lock, então as notificações saem uma de cada vez.
 *
 * <p>Por que escrito à mão e não Resilience4j: ver docs/adr/0003.
 */
public class NotificacaoComDisjuntor implements NotificacaoPort {

    public static final String TIPO_PENDENTE = "NOTIFICACAO_PENDENTE";

    private static final Logger log = LoggerFactory.getLogger(NotificacaoComDisjuntor.class);

    private final NotificacaoPort proximo;
    private final int limiteFalhas;
    private final Duration tempoAberto;
    private final LogEventoService logEventoService;
    private final Clock relogio;

    private int falhasSeguidas = 0;
    private Instant abertoAte = null;

    public NotificacaoComDisjuntor(NotificacaoPort proximo, int limiteFalhas, Duration tempoAberto,
                                   LogEventoService logEventoService) {
        this(proximo, limiteFalhas, tempoAberto, logEventoService, Clock.systemUTC());
    }

    /** Para testes: o relógio é injetado, e "esperar 30 s" é só avançá-lo. */
    public NotificacaoComDisjuntor(NotificacaoPort proximo, int limiteFalhas, Duration tempoAberto,
                                   LogEventoService logEventoService, Clock relogio) {
        this.proximo = proximo;
        this.limiteFalhas = Math.max(1, limiteFalhas);
        this.tempoAberto = tempoAberto;
        this.logEventoService = logEventoService;
        this.relogio = relogio;
    }

    @Override
    public synchronized void notificar(Long usuarioId, String mensagem) {
        if (abertoAte != null && relogio.instant().isBefore(abertoAte)) {      // ABERTO
            registrarPendente(usuarioId, mensagem);
            return;
        }
        boolean meioAberto = abertoAte != null;                                  // o tempo passou
        try {
            proximo.notificar(usuarioId, mensagem);
            if (meioAberto) {
                log.info("Disjuntor FECHADO: o serviço voltou");
            }
            falhasSeguidas = 0;
            abertoAte = null;
        } catch (NotificacaoIndisponivelException ex) {
            if (meioAberto || ++falhasSeguidas >= limiteFalhas) {
                abertoAte = relogio.instant().plus(tempoAberto);
                log.warn("Disjuntor ABERTO até {}", abertoAte);
            }
            throw ex;                                                            // o retry, por fora, decide
        }
    }

    /** Estado atual, para logs e testes: FECHADO, ABERTO ou MEIO_ABERTO. */
    public synchronized String estado() {
        if (abertoAte == null) {
            return "FECHADO";
        }
        return relogio.instant().isBefore(abertoAte) ? "ABERTO" : "MEIO_ABERTO";
    }

    /**
     * O fallback: a notificação não sai, mas fica registrada para depois. O
     * registro também pode falhar (o Mongo pode estar fora), por isso o
     * try/catch: um fallback que lança exceção não é fallback.
     */
    private void registrarPendente(Long usuarioId, String mensagem) {
        log.warn("Disjuntor aberto: notificação para o usuário {} ficou pendente", usuarioId);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("usuarioId", usuarioId);
            payload.put("mensagem", mensagem);
            payload.put("abertoAte", String.valueOf(abertoAte));
            logEventoService.registrarAviso(TIPO_PENDENTE, "NotificacaoComDisjuntor",
                    "Notificação para o usuário " + usuarioId + " pendente: disjuntor aberto", payload);
        } catch (Exception ex) {
            log.error("Não foi possível registrar a pendência", ex);
        }
    }
}
