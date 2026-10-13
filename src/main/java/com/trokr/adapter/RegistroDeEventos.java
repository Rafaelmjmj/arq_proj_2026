package com.trokr.adapter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Adapter — a INTERFACE ALVO (na Arquitetura Hexagonal, a "porta").
 *
 * Define o vocabulário que o resto do sistema usa para registrar e consultar
 * eventos: registrar(), buscarPorTipo()... Nenhuma menção a MongoDB, JPA ou
 * qualquer tecnologia. Hoje a única implementação é
 * {@code RegistroDeEventosMongo}; trocar de banco (Elasticsearch, arquivo,
 * outro) seria escrever um novo adaptador — LogEventoService, os listeners
 * e o controller não mudariam uma linha.
 */
public interface RegistroDeEventos {

    /** Grava o evento e devolve como ficou salvo (com id). */
    EventoRegistrado registrar(EventoRegistrado evento);

    List<EventoRegistrado> buscarPorTipo(String tipo);

    /** Ex.: "erros das últimas 24h" = buscarPorNivelDesde(ERROR, agora - 24h). */
    List<EventoRegistrado> buscarPorNivelDesde(NivelLog nivel, LocalDateTime desde);
}
