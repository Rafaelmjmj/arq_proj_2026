package com.trokr.service;

import com.trokr.adapter.EventoRegistrado;
import com.trokr.adapter.NivelLog;
import com.trokr.adapter.RegistroDeEventos;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário de log do sistema (Aula 8) — qualquer Service,
 * listener ou handler pode chamá-lo.
 *
 * Depende SÓ da interface {@link RegistroDeEventos} (Adapter). Não sabe que
 * por baixo é MongoDB: nenhum import de Mongo, nenhum insert().
 *
 * Log nunca derruba regra de negócio: se o armazenamento de log falhar
 * (Mongo fora do ar, por exemplo), o erro vai para o console e a operação
 * principal — uma troca, por exemplo — segue normalmente.
 */
@Service
@RequiredArgsConstructor
public class LogEventoService {

    public static final String TIPO_ERRO_SISTEMA = "ERRO_SISTEMA";

    private static final Logger log = LoggerFactory.getLogger(LogEventoService.class);

    private final RegistroDeEventos registroDeEventos;

    public void registrarInfo(String tipo, String origem, String mensagem, Map<String, Object> payload, Long usuarioId) {
        registrar(EventoRegistrado.novo(tipo, NivelLog.INFO, origem, mensagem, payload, usuarioId));
    }

    public void registrarAviso(String tipo, String origem, String mensagem, Map<String, Object> payload) {
        registrar(EventoRegistrado.novo(tipo, NivelLog.WARN, origem, mensagem, payload, null));
    }

    /** Erro de sistema: o payload guarda a exceção. Sem usuário associado. */
    public void registrarErro(String origem, String mensagem, Exception excecao) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("excecao", excecao.getClass().getName());
        payload.put("mensagemExcecao", String.valueOf(excecao.getMessage()));
        registrar(EventoRegistrado.novo(TIPO_ERRO_SISTEMA, NivelLog.ERROR, origem, mensagem, payload, null));
    }

    public List<EventoRegistrado> buscarPorTipo(String tipo) {
        return registroDeEventos.buscarPorTipo(tipo);
    }

    /** Ex.: buscarPorNivelNasUltimasHoras(ERROR, 24) = "erros das últimas 24h". */
    public List<EventoRegistrado> buscarPorNivelNasUltimasHoras(NivelLog nivel, int horas) {
        return registroDeEventos.buscarPorNivelDesde(nivel, LocalDateTime.now().minusHours(horas));
    }

    private void registrar(EventoRegistrado evento) {
        try {
            registroDeEventos.registrar(evento);
        } catch (RuntimeException falha) {
            log.warn("Não foi possível gravar o LogEvento {} ({}): {}",
                    evento.tipo(), evento.mensagem(), falha.getMessage());
        }
    }
}
