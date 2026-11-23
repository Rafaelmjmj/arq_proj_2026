package com.trokr.notificacao;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * O interruptor da demonstração (Aula 13): serviço de notificação no ar ou
 * fora do ar, quando quisermos. Testar com sorteio é imprevisível; com a
 * chave, fora do ar TODA chamada falha e no ar nenhuma falha.
 *
 * AtomicBoolean porque várias requisições leem a chave ao mesmo tempo que o
 * endpoint de demonstração a muda (Aula 10).
 */
@Component
public class SimulacaoNotificacao {

    private final AtomicBoolean foraDoAr = new AtomicBoolean(false);

    public boolean foraDoAr() {
        return foraDoAr.get();
    }

    public void derrubar() {
        foraDoAr.set(true);
    }

    public void religar() {
        foraDoAr.set(false);
    }
}
