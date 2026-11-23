package com.trokr.controller;

import com.trokr.adapter.NivelLog;
import com.trokr.dto.LogEventoResponseDTO;
import com.trokr.service.LogEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta do log do sistema (MongoDB) sem abrir o Compass. O controller
 * depende do LogEventoService, que depende da porta RegistroDeEventos —
 * ninguém aqui sabe que por baixo é Mongo.
 */
@Tag(name = "Logs", description = "Log de eventos do sistema, guardado no MongoDB atrás da porta RegistroDeEventos. "
        + "Tipos úteis: TROCA_CONCLUIDA, TRANSICAO_INVALIDA, ERRO_SISTEMA, NOTIFICACAO_PENDENTE.")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogEventoController {

    private final LogEventoService logEventoService;

    /** Ex.: GET /logs?tipo=TROCA_CONCLUIDA */
    @Operation(summary = "Lista eventos de um tipo")
    @GetMapping
    public List<LogEventoResponseDTO> listarPorTipo(
            @Parameter(description = "Tipo do evento", example = "NOTIFICACAO_PENDENTE") @RequestParam String tipo) {
        return logEventoService.buscarPorTipo(tipo).stream()
                .map(LogEventoResponseDTO::fromEvento)
                .toList();
    }

    /** Ex.: GET /logs/nivel?nivel=ERROR&horas=24 — "erros das últimas 24h". */
    @Operation(summary = "Lista eventos de um nível nas últimas horas",
            description = "Ex.: nivel=ERROR e horas=24.")
    @GetMapping("/nivel")
    public List<LogEventoResponseDTO> listarPorNivel(@RequestParam NivelLog nivel,
                                                     @Parameter(description = "Janela em horas", example = "24") @RequestParam(defaultValue = "24") int horas) {
        return logEventoService.buscarPorNivelNasUltimasHoras(nivel, horas).stream()
                .map(LogEventoResponseDTO::fromEvento)
                .toList();
    }
}
