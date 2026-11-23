package com.trokr.notificacao;

import com.trokr.service.LogEventoService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Monta a pilha de Decorators da notificação (Aula 13):
 *
 * <pre>
 *   NotificarUsuariosListener ──▶ NotificacaoComRetry        (por fora)
 *                                   └▶ NotificacaoComDisjuntor (por dentro)
 *                                        └▶ NotificacaoSimuladaAdapter (centro)
 * </pre>
 *
 * Todas são NotificacaoPort. O listener recebe uma porta e chama
 * notificar(); não sabe quantas camadas existem por dentro.
 *
 * <p>@Primary: há duas NotificacaoPort no contexto (o adaptador, que é
 * @Component, e esta pilha). Quem pede uma NotificacaoPort recebe a pilha.
 *
 * <p>Por que o disjuntor fica POR DENTRO do retry: cada tentativa passa por
 * ele e é contada; quando ele abre, devolve a pendência sem lançar, e o retry
 * para na hora. Na ordem inversa, o disjuntor só veria a falha final de cada
 * pessoa, depois de todas as esperas do retry.
 */
@Configuration
@EnableConfigurationProperties(NotificacaoProperties.class)
public class NotificacaoConfig {

    @Bean
    @Primary
    public NotificacaoPort notificacaoPort(NotificacaoSimuladaAdapter adaptador,
                                           NotificacaoProperties props,
                                           LogEventoService logEventoService) {
        // de fora para dentro: retry -> disjuntor -> adaptador
        return new NotificacaoComRetry(
                new NotificacaoComDisjuntor(adaptador, props.limiteFalhas(), props.tempoAberto(), logEventoService),
                props.maxTentativas(), props.esperaInicialMs());
    }
}
