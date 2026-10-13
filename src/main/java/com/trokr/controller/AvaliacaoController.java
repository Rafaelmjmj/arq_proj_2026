package com.trokr.controller;

import com.trokr.dto.AvaliacaoResponseDTO;
import com.trokr.dto.AvaliarRequestDTO;
import com.trokr.service.AvaliacaoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avaliações nascem PENDENTES quando uma troca é concluída (Observer).
 * Aqui só se consulta e se preenche — não existe POST.
 */
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    /** Filtros opcionais: ?propostaId=, ?usuarioAlvoId= ou ?usuarioAutorId=. */
    @GetMapping
    public List<AvaliacaoResponseDTO> listar(@RequestParam(required = false) Long propostaId,
                                             @RequestParam(required = false) Long usuarioAlvoId,
                                             @RequestParam(required = false) Long usuarioAutorId) {
        return avaliacaoService.listar(propostaId, usuarioAlvoId, usuarioAutorId).stream()
                .map(AvaliacaoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public AvaliacaoResponseDTO buscarPorId(@PathVariable Long id) {
        return AvaliacaoResponseDTO.fromEntity(avaliacaoService.buscarPorId(id));
    }

    /** Preenche a avaliação: PENDENTE → AVALIADA, só pelo autor, uma única vez. */
    @PatchMapping("/{id}")
    public AvaliacaoResponseDTO avaliar(@PathVariable Long id, @Valid @RequestBody AvaliarRequestDTO dto) {
        return AvaliacaoResponseDTO.fromEntity(
                avaliacaoService.avaliar(id, dto.usuarioId(), dto.nota(), dto.descricao()));
    }
}
