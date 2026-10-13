package com.trokr.adapter.mongo;

import com.trokr.adapter.NivelLog;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Log genérico do sistema, gravado como DOCUMENTO no MongoDB (Aula 8).
 *
 * Genérico de propósito: não é "log de troca", é log de qualquer coisa.
 * O {@code payload} não tem schema fixo — um TROCA_CONCLUIDA guarda ids de
 * usuários e itens; um ERRO_SISTEMA guarda a exceção. Mesma coleção, sem
 * migração e sem colunas nulas.
 *
 * Fica dentro do pacote do adaptador: só o RegistroDeEventosMongo conhece
 * esta classe. O resto do sistema fala com EventoRegistrado.
 */
@Document(collection = "log_evento")
@Getter
@Setter
@NoArgsConstructor
public class LogEvento {

    @Id
    private String id;

    private String tipo;

    private NivelLog nivel;

    private String origem;

    private String mensagem;

    private Map<String, Object> payload;

    private LocalDateTime timestamp;

    private Long usuarioId;
}
