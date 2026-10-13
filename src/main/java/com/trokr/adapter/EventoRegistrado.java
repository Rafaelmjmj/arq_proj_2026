package com.trokr.adapter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Um evento de log no vocabulário do DOMÍNIO — sem nenhuma anotação de
 * banco. É o que atravessa a porta {@link RegistroDeEventos}: quem usa a
 * porta nunca vê o documento do Mongo ({@code LogEvento}).
 *
 * @param id        gerado pelo armazenamento (null antes de registrar)
 * @param tipo      ex.: TROCA_CONCLUIDA, TRANSICAO_INVALIDA, ERRO_SISTEMA
 * @param payload   dados específicos do tipo, sem schema fixo
 * @param usuarioId opcional — nem todo evento tem usuário associado
 */
public record EventoRegistrado(
        String id,
        String tipo,
        NivelLog nivel,
        String origem,
        String mensagem,
        Map<String, Object> payload,
        LocalDateTime timestamp,
        Long usuarioId
) {

    /** Monta um evento novo (ainda sem id), com timestamp de agora. */
    public static EventoRegistrado novo(String tipo, NivelLog nivel, String origem, String mensagem,
                                        Map<String, Object> payload, Long usuarioId) {
        return new EventoRegistrado(null, tipo, nivel, origem, mensagem,
                payload == null ? Map.of() : payload, LocalDateTime.now(), usuarioId);
    }
}
