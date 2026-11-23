package com.trokr.controller;

import com.trokr.dto.ContrapropostaRequestDTO;
import com.trokr.dto.PropostaRequestDTO;
import com.trokr.dto.PropostaResponseDTO;
import com.trokr.model.Proposta;
import com.trokr.service.PropostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fala SÓ com o PropostaService (Facade): nenhum repositório, estado ou
 * publisher de evento aqui, e nenhuma regra de negócio — só HTTP ↔ DTO.
 *
 * Toda transição de estado é um PATCH (modifica parcialmente o recurso:
 * só o status). Quando a ação depende de QUEM está agindo (aceitar,
 * recusar, desistir), o id do usuário vem em ?usuarioId= — substituto do
 * usuário logado enquanto não existe autenticação.
 */
@Tag(name = "Propostas", description = "Propostas e contrapropostas têm um CICLO DE VIDA (State Pattern): "
        + "Rascunho → Homologação → Ativa → Negociado → Finalizado (raiz) e Rascunho → Em Análise → Negociado → Finalizado "
        + "(contraproposta). Uma ação fora de hora responde 409 com um ProblemDetail. "
        + "Ações que dependem de quem age recebem ?usuarioId= (ainda não há login).")
@RestController
@RequestMapping("/propostas")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    // ---------------- consultas ----------------

    /** Lista todas; com ?apenasRaizes=true, só as propostas originais. */
    @Operation(summary = "Lista propostas",
            description = "Com apenasRaizes=true, só as propostas originais (sem as contrapropostas).")
    @GetMapping
    public List<PropostaResponseDTO> listar(@RequestParam(defaultValue = "false") boolean apenasRaizes) {
        List<Proposta> propostas = apenasRaizes ? propostaService.listarRaizes() : propostaService.listarTodas();
        return paraDTO(propostas);
    }

    @Operation(summary = "Busca uma proposta ou contraproposta")
    @ApiResponse(responseCode = "200", description = "Proposta encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public PropostaResponseDTO buscarPorId(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.buscarPorId(id));
    }

    @Operation(summary = "Lista as propostas criadas por um usuário")
    @GetMapping("/usuario/{usuarioId}")
    public List<PropostaResponseDTO> listarPorUsuario(
            @Parameter(description = "Id do autor", example = "1") @PathVariable Long usuarioId) {
        return paraDTO(propostaService.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Lista as contrapropostas concorrentes de uma raiz")
    @GetMapping("/{id}/contrapropostas")
    public List<PropostaResponseDTO> listarContrapropostas(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return paraDTO(propostaService.listarContrapropostas(id));
    }

    // ---------------- criação ----------------

    @Operation(summary = "Cria uma proposta raiz",
            description = "Nasce em RASCUNHO. O item oferecido precisa ser do autor.")
    @ApiResponse(responseCode = "201", description = "Proposta criada em RASCUNHO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "USUARIO_NAO_ENCONTRADO ou ITEM_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O item não é do autor (ITEM_DE_OUTRO_USUARIO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<PropostaResponseDTO> criar(@Valid @RequestBody PropostaRequestDTO dto) {
        Proposta criada = propostaService.criarProposta(dto.titulo(), dto.descricao(), dto.usuarioId(), dto.itemId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PropostaResponseDTO.fromEntity(criada));
    }

    /** Contrapropor = criar uma Proposta nova apontando para esta (autorrelacionamento). */
    @Operation(summary = "Contrapropõe uma proposta ativa",
            description = "Cria uma contraproposta em RASCUNHO, ligada à raiz.")
    @ApiResponse(responseCode = "201", description = "Contraproposta criada em RASCUNHO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA, USUARIO_NAO_ENCONTRADO ou ITEM_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A proposta não está Ativa (PROPOSTA_NAO_RECEBE_CONTRAPROPOSTA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Contraproposta para si mesmo, de outra contraproposta ou com item de outro usuário",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/contrapropostas")
    public ResponseEntity<PropostaResponseDTO> contrapropor(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id,
                                                            @Valid @RequestBody ContrapropostaRequestDTO dto) {
        Proposta contra = propostaService.criarContraproposta(id, dto.descricao(), dto.usuarioId(), dto.itemId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PropostaResponseDTO.fromEntity(contra));
    }

    // ---------------- transições (State) ----------------

    /** Raiz: Rascunho → Homologação. Contraproposta: Rascunho → Em Análise. */
    @Operation(summary = "Submete",
            description = "Raiz: Rascunho → Homologação. Contraproposta: Rascunho → Em Análise.")
    @ApiResponse(responseCode = "200", description = "Novo estado na resposta",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/submeter")
    public PropostaResponseDTO submeter(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.submeter(id));
    }

    /** Raiz: Homologação → Ativa. */
    @Operation(summary = "Aprova uma proposta raiz",
            description = "Homologação → Ativa: passa a receber contrapropostas.")
    @ApiResponse(responseCode = "200", description = "Proposta Ativa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Contrapropostas não são aprovadas (ACAO_EXCLUSIVA_DA_RAIZ)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/aprovar")
    public PropostaResponseDTO aprovar(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.aprovar(id));
    }

    /** Volta para edição (Rascunho). */
    @Operation(summary = "Volta para Rascunho, para edição")
    @ApiResponse(responseCode = "200", description = "Proposta em Rascunho",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/editar")
    public PropostaResponseDTO voltarParaRascunho(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.voltarParaRascunho(id));
    }

    /** Contraproposta: o dono da raiz aceita. Contra e raiz vão para Negociado. */
    @Operation(summary = "Aceita uma contraproposta",
            description = "Só o dono da raiz. Raiz e contraproposta vão para Negociado.")
    @ApiResponse(responseCode = "200", description = "Contraproposta em Negociado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é o dono da raiz (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Ação de contraproposta pedida numa raiz (ACAO_EXCLUSIVA_DA_CONTRAPROPOSTA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/aceitar")
    public PropostaResponseDTO aceitar(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id,
            @Parameter(description = "Quem está agindo", example = "1") @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.aceitar(id, usuarioId));
    }

    /** Contraproposta em análise: o dono da raiz recusa. */
    @Operation(summary = "Recusa uma contraproposta em análise",
            description = "Só o dono da raiz.")
    @ApiResponse(responseCode = "200", description = "Contraproposta Recusada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é o dono da raiz (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Ação de contraproposta pedida numa raiz (ACAO_EXCLUSIVA_DA_CONTRAPROPOSTA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/recusar")
    public PropostaResponseDTO recusar(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id,
            @Parameter(description = "Quem está agindo", example = "1") @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.recusar(id, usuarioId));
    }

    /** Contraproposta em negociação: uma das partes desiste; a raiz volta para Ativa. */
    @Operation(summary = "Desiste de uma negociação",
            description = "Qualquer uma das partes. A raiz volta para Ativa.")
    @ApiResponse(responseCode = "200", description = "Contraproposta Cancelada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é parte da negociação (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Ação de contraproposta pedida numa raiz (ACAO_EXCLUSIVA_DA_CONTRAPROPOSTA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/desistir")
    public PropostaResponseDTO desistir(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id,
            @Parameter(description = "Quem está agindo", example = "1") @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.desistirDaNegociacao(id, usuarioId));
    }

    @Operation(summary = "Cancela uma proposta")
    @ApiResponse(responseCode = "200", description = "Proposta Cancelada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/cancelar")
    public PropostaResponseDTO cancelar(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.cancelar(id));
    }

    /**
     * Finaliza a negociação (id da raiz ou da contraproposta em negociação).
     * Uma linha aqui — todo o resto acontece por trás da Facade.
     */
    @Operation(summary = "Finaliza a troca (Facade)",
            description = "Uma chamada: as duas máquinas vão para Finalizado, as contrapropostas concorrentes são canceladas e o evento TrocaConcluida dispara (créditos, avaliações, histórico, log e notificação). Aceita o id da raiz ou da contraproposta em negociação.")
    @ApiResponse(responseCode = "200", description = "Troca concluída: raiz em Finalizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA ou CONTRAPROPOSTA_TRANSICAO_INVALIDA), sem negociação (PROPOSTA_SEM_NEGOCIACAO) ou finalizada ao mesmo tempo por outra requisição (PROPOSTA_ALTERADA_CONCORRENTEMENTE)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/finalizar")
    public PropostaResponseDTO finalizar(
            @Parameter(description = "Id da proposta (raiz ou contraproposta)", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.finalizarNegociacao(id));
    }

    private List<PropostaResponseDTO> paraDTO(List<Proposta> propostas) {
        return propostas.stream().map(PropostaResponseDTO::fromEntity).toList();
    }
}
