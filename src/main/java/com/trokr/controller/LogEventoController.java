package com.trokr.controller;

import com.trokr.adapter.NivelLog;
import com.trokr.dto.LogEventoResponseDTO;
import com.trokr.service.LogEventoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta do log do sistema (MongoDB) sem abrir o Compass. O controller
 * depende do LogEventoService, que depende da porta RegistroDeEventos —
 * ninguém aqui sabe que por baixo é Mongo.
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogEventoController {

    private final LogEventoService logEventoService;

    /** Ex.: GET /logs?tipo=TROCA_CONCLUIDA */
    @GetMapping
    public List<LogEventoResponseDTO> listarPorTipo(@RequestParam String tipo) {
        return logEventoService.buscarPorTipo(tipo).stream()
                .map(LogEventoResponseDTO::fromEvento)
                .toList();
    }

    /** Ex.: GET /logs/nivel?nivel=ERROR&horas=24 — "erros das últimas 24h". */
    @GetMapping("/nivel")
    public List<LogEventoResponseDTO> listarPorNivel(@RequestParam NivelLog nivel,
                                                     @RequestParam(defaultValue = "24") int horas) {
        return logEventoService.buscarPorNivelNasUltimasHoras(nivel, horas).stream()
                .map(LogEventoResponseDTO::fromEvento)
                .toList();
    }
}
