package com.trokr.controller;

import com.trokr.dto.AvaliacaoResponseDTO;
import com.trokr.dto.AvaliarRequestDTO;
import com.trokr.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Avaliações", description = "Nascem PENDENTES quando uma troca é finalizada (uma para cada parte). "
        + "Aqui só se consulta e se preenche, uma única vez, pelo autor.")
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    /** Filtros opcionais: ?propostaId=, ?usuarioAlvoId= ou ?usuarioAutorId=. */
    @Operation(summary = "Lista avaliações",
            description = "Filtros opcionais por proposta, alvo ou autor.")
    @GetMapping
    public List<AvaliacaoResponseDTO> listar(
            @Parameter(description = "Proposta raiz", example = "1") @RequestParam(required = false) Long propostaId,
                                             @RequestParam(required = false) Long usuarioAlvoId,
                                             @RequestParam(required = false) Long usuarioAutorId) {
        return avaliacaoService.listar(propostaId, usuarioAlvoId, usuarioAutorId).stream()
                .map(AvaliacaoResponseDTO::fromEntity)
                .toList();
    }

    @Operation(summary = "Busca uma avaliação")
    @ApiResponse(responseCode = "200", description = "Avaliação encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvaliacaoResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "AVALIACAO_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public AvaliacaoResponseDTO buscarPorId(
            @Parameter(description = "Id da avaliação", example = "1") @PathVariable Long id) {
        return AvaliacaoResponseDTO.fromEntity(avaliacaoService.buscarPorId(id));
    }

    /** Preenche a avaliação: PENDENTE → AVALIADA, só pelo autor, uma única vez. */
    @Operation(summary = "Preenche uma avaliação",
            description = "PENDENTE → AVALIADA, só pelo autor, nota de 1 a 5.")
    @ApiResponse(responseCode = "200", description = "Avaliação registrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvaliacaoResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Quem preenche não é o autor (AVALIACAO_AUTOR_INVALIDO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "AVALIACAO_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A avaliação já foi preenchida (AVALIACAO_JA_PREENCHIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Nota fora de 1 a 5 (AVALIACAO_NOTA_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    public AvaliacaoResponseDTO avaliar(
            @Parameter(description = "Id da avaliação", example = "1") @PathVariable Long id,
            @Valid @RequestBody AvaliarRequestDTO dto) {
        return AvaliacaoResponseDTO.fromEntity(
                avaliacaoService.avaliar(id, dto.usuarioId(), dto.nota(), dto.descricao()));
    }
}
