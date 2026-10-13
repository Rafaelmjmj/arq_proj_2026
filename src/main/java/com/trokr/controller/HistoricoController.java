package com.trokr.controller;

import com.trokr.dto.HistoricoResponseDTO;
import com.trokr.service.HistoricoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/historicos")
@RequiredArgsConstructor
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    public List<HistoricoResponseDTO> listarPorProposta(@RequestParam Long propostaId) {
        return historicoService.listarPorProposta(propostaId).stream()
                .map(HistoricoResponseDTO::fromEntity)
                .toList();
    }
}
