package com.trokr.controller;

import com.trokr.dto.CancelarRequestDTO;
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
@Tag(name = "Propostas", description = "Propostas e contrapropostas têm um CICLO DE VIDA (State Pattern). "
        + "Raiz: RASCUNHO → HOMOLOGACAO → ATIVA → NEGOCIADO_RAIZ → FINALIZADA. "
        + "Contraproposta: RASCUNHO → EM_ANALISE → NEGOCIADO_CONTRA (ou RECUSADO, ou CANCELADO_CONTRA). "
        + "Uma ação fora de hora responde 409 com um ProblemDetail.")
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

    // ---------------- ações da máquina de estados ----------------
    //
    // As rotas seguem o roteiro de testes do professor (projeto de referência):
    // ações por POST, finalização por PATCH. Uma ação fora de hora responde 409.

    @Operation(summary = "Envia a proposta raiz para homologação",
            description = "RASCUNHO → HOMOLOGACAO.")
    @ApiResponse(responseCode = "200", description = "Proposta em HOMOLOGACAO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/enviar-homologacao")
    public PropostaResponseDTO enviarHomologacao(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.enviarParaHomologacao(id));
    }

    @Operation(summary = "Recusa a homologação",
            description = "HOMOLOGACAO → RASCUNHO, para o autor corrigir e reenviar.")
    @ApiResponse(responseCode = "200", description = "Proposta de volta a RASCUNHO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/recusar-homologacao")
    public PropostaResponseDTO recusarHomologacao(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.recusarHomologacao(id));
    }

    @Operation(summary = "Aprova a proposta raiz",
            description = "HOMOLOGACAO → ATIVA: passa a receber contrapropostas. Numa contraproposta, 409.")
    @ApiResponse(responseCode = "200", description = "Proposta ATIVA",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/aprovar")
    public PropostaResponseDTO aprovar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.aprovar(id));
    }

    @Operation(summary = "Volta a proposta para edição",
            description = "Raiz: HOMOLOGACAO ou ATIVA → RASCUNHO. Contraproposta: EM_ANALISE → RASCUNHO.")
    @ApiResponse(responseCode = "200", description = "Proposta em RASCUNHO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/editar")
    public PropostaResponseDTO voltarParaRascunho(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.voltarParaRascunho(id));
    }

    @Operation(summary = "Envia a contraproposta para análise",
            description = "RASCUNHO → EM_ANALISE.")
    @ApiResponse(responseCode = "200", description = "Contraproposta EM_ANALISE",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora (CONTRAPROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/enviar")
    public PropostaResponseDTO enviar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.enviarParaAnalise(id));
    }

    @Operation(summary = "Aceita uma contraproposta (1ª etapa)",
            description = "EM_ANALISE → NEGOCIADO_CONTRA. A raiz continua ATIVA até o dono confirmar em /aceitar-contraproposta. Só uma contraproposta aceita por vez.")
    @ApiResponse(responseCode = "200", description = "Contraproposta em NEGOCIADO_CONTRA",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é o dono da raiz (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Fora de hora, ou já existe outra contraproposta aceita",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/aceitar")
    public PropostaResponseDTO aceitar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id,
            @Parameter(description = "Opcional: quem está agindo. Se informado, precisa ser o dono da raiz (403)", example = "1") @RequestParam(required = false) Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.aceitar(id, usuarioId));
    }

    @Operation(summary = "Confirma a contraproposta aceita (2ª etapa)",
            description = "Na proposta raiz: ATIVA → NEGOCIADO_RAIZ. Exige uma contraproposta em NEGOCIADO_CONTRA.")
    @ApiResponse(responseCode = "200", description = "Proposta em NEGOCIADO_RAIZ",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é o dono da proposta (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Fora de hora ou sem contraproposta aceita (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/aceitar-contraproposta")
    public PropostaResponseDTO aceitarContraproposta(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id,
            @Parameter(description = "Opcional: quem está agindo. Se informado, precisa ser o dono da raiz (403)", example = "1") @RequestParam(required = false) Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.aceitarContraproposta(id, usuarioId));
    }

    @Operation(summary = "Recusa uma contraproposta em análise",
            description = "EM_ANALISE → RECUSADO.")
    @ApiResponse(responseCode = "200", description = "Contraproposta RECUSADO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é o dono da raiz (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/recusar")
    public PropostaResponseDTO recusar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id,
            @Parameter(description = "Opcional: quem está agindo. Se informado, precisa ser o dono da raiz (403)", example = "1") @RequestParam(required = false) Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.recusar(id, usuarioId));
    }

    @Operation(summary = "Desiste de uma contraproposta aceita",
            description = "Qualquer uma das partes, antes da finalização. A contraproposta vai para RECUSADO e a raiz volta a ATIVA.")
    @ApiResponse(responseCode = "200", description = "Contraproposta RECUSADO",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Quem age não é parte da negociação (PROPOSTA_ACAO_NAO_AUTORIZADA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/desistir")
    public PropostaResponseDTO desistir(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id,
            @Parameter(description = "Quem está agindo", example = "1") @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.desistirDaNegociacao(id, usuarioId));
    }

    @Operation(summary = "Cancela uma proposta ou contraproposta",
            description = "Contraproposta: exige justificativa. Raiz: justificativa opcional, e as contrapropostas abertas são encerradas (cascata).")
    @ApiResponse(responseCode = "200", description = "Cancelada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ação fora de hora para o estado atual (PROPOSTA_TRANSICAO_INVALIDA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Contraproposta sem justificativa (JUSTIFICATIVA_OBRIGATORIA)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/cancelar")
    public PropostaResponseDTO cancelar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id,
            @RequestBody(required = false) CancelarRequestDTO dto) {
        return PropostaResponseDTO.fromEntity(propostaService.cancelar(id, dto == null ? null : dto.justificativa()));
    }

    /**
     * Aula 9 — o cliente da Facade: uma linha aqui, todo o resto acontece por
     * trás do PropostaService.
     */
    @Operation(summary = "Finaliza a troca (Facade)",
            description = "A raiz precisa estar em NEGOCIADO_RAIZ e vai para FINALIZADA; a contraproposta vencedora continua em NEGOCIADO_CONTRA e as concorrentes abertas são canceladas. Dispara o TrocaConcluidaEvent (créditos, avaliações, histórico, log e notificação).")
    @ApiResponse(responseCode = "200", description = "Troca concluída: raiz FINALIZADA",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "PROPOSTA_NAO_ENCONTRADA",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Fora de hora (PROPOSTA_TRANSICAO_INVALIDA) ou finalizada ao mesmo tempo por outra requisição (PROPOSTA_ALTERADA_CONCORRENTEMENTE)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/finalizar")
    public PropostaResponseDTO finalizar(
            @Parameter(description = "Id da proposta", example = "1") @PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.finalizarNegociacao(id));
    }

    private List<PropostaResponseDTO> paraDTO(List<Proposta> propostas) {
        return propostas.stream().map(PropostaResponseDTO::fromEntity).toList();
    }
}
