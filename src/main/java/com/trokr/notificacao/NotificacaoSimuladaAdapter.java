package com.trokr.notificacao;

import com.trokr.config.AtrasoDemo;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * O adaptador da {@link NotificacaoPort} (Aula 13) — antes era o
 * NotificacaoService da Aula 11. O corpo é o mesmo: espera a latência,
 * sorteia contra a taxa de falha, lança NotificacaoIndisponivelException ou
 * registra o envio. Novidade: a primeira linha consulta o interruptor
 * {@link SimulacaoNotificacao}.
 *
 * <p>Trocar de provedor (e-mail, SMS, push) é escrever outro adaptador da
 * mesma porta; o listener e as camadas de proteção não mudam.
 *
 * <p>É o único @Component que implementa a porta. Quem pede uma
 * NotificacaoPort recebe a pilha montada na {@link NotificacaoConfig}
 * (@Primary), não este adaptador puro.
 */
@Component
public class NotificacaoSimuladaAdapter implements NotificacaoPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoSimuladaAdapter.class);

    private final NotificacaoProperties props;
    private final SimulacaoNotificacao simulacao;
    private final DoubleSupplier sorteio;

    @Autowired
    public NotificacaoSimuladaAdapter(NotificacaoProperties props, SimulacaoNotificacao simulacao) {
        this(props, simulacao, () -> ThreadLocalRandom.current().nextDouble());
    }

    /** Para testes: o sorteio é injetado e o resultado fica previsível. */
    public NotificacaoSimuladaAdapter(NotificacaoProperties props, SimulacaoNotificacao simulacao,
                                      DoubleSupplier sorteio) {
        this.props = props;
        this.simulacao = simulacao;
        this.sorteio = sorteio;
    }

    @Override
    public void notificar(Long usuarioId, String mensagem) {
        if (simulacao.foraDoAr()) {
            throw new NotificacaoIndisponivelException("Serviço de notificação fora do ar");
        }
        if (props.latenciaMs() > 0) {
            AtrasoDemo.dormir(props.latenciaMs());
        }
        if (sorteio.getAsDouble() < props.taxaFalha()) {
            throw new NotificacaoIndisponivelException(
                    "Serviço de notificação indisponível ao avisar o usuário " + usuarioId);
        }
        log.info("[NOTIFICACAO ENVIADA] usuario={} | {}", usuarioId, mensagem);
    }
}
