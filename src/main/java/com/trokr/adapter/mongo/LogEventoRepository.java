package com.trokr.adapter.mongo;

import com.trokr.adapter.NivelLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB — mesma ideia dos query methods do JPA, agora sobre
 * documentos. Chave String (ObjectId do Mongo), não Long.
 */
public interface LogEventoRepository extends MongoRepository<LogEvento, String> {

    List<LogEvento> findByTipoOrderByTimestampDesc(String tipo);

    List<LogEvento> findByNivelAndTimestampAfterOrderByTimestampDesc(NivelLog nivel, LocalDateTime desde);
}
