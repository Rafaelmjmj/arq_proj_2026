package com.trokr.controller;

import com.trokr.dto.HistoricoResponseDTO;
import com.trokr.service.HistoricoService;
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

@Tag(name = "Histórico", description = "Registro permanente de cada troca concluída (criado pelo Observer).")
@RestController
@RequestMapping("/historicos")
@RequiredArgsConstructor
public class HistoricoController {

    private final HistoricoService historicoService;

    @Operation(summary = "Lista o histórico de uma proposta raiz")
    @GetMapping
    public List<HistoricoResponseDTO> listarPorProposta(
            @Parameter(description = "Proposta raiz", example = "1") @RequestParam Long propostaId) {
        return historicoService.listarPorProposta(propostaId).stream()
                .map(HistoricoResponseDTO::fromEntity)
                .toList();
    }
}
