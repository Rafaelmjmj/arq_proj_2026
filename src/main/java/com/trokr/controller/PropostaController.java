package com.trokr.controller;

import com.trokr.dto.ContrapropostaRequestDTO;
import com.trokr.dto.PropostaRequestDTO;
import com.trokr.dto.PropostaResponseDTO;
import com.trokr.model.Proposta;
import com.trokr.service.PropostaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RestController
@RequestMapping("/propostas")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    // ---------------- consultas ----------------

    /** Lista todas; com ?apenasRaizes=true, só as propostas originais. */
    @GetMapping
    public List<PropostaResponseDTO> listar(@RequestParam(defaultValue = "false") boolean apenasRaizes) {
        List<Proposta> propostas = apenasRaizes ? propostaService.listarRaizes() : propostaService.listarTodas();
        return paraDTO(propostas);
    }

    @GetMapping("/{id}")
    public PropostaResponseDTO buscarPorId(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.buscarPorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<PropostaResponseDTO> listarPorUsuario(@PathVariable Long usuarioId) {
        return paraDTO(propostaService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}/contrapropostas")
    public List<PropostaResponseDTO> listarContrapropostas(@PathVariable Long id) {
        return paraDTO(propostaService.listarContrapropostas(id));
    }

    // ---------------- criação ----------------

    @PostMapping
    public ResponseEntity<PropostaResponseDTO> criar(@Valid @RequestBody PropostaRequestDTO dto) {
        Proposta criada = propostaService.criarProposta(dto.titulo(), dto.descricao(), dto.usuarioId(), dto.itemId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PropostaResponseDTO.fromEntity(criada));
    }

    /** Contrapropor = criar uma Proposta nova apontando para esta (autorrelacionamento). */
    @PostMapping("/{id}/contrapropostas")
    public ResponseEntity<PropostaResponseDTO> contrapropor(@PathVariable Long id,
                                                            @Valid @RequestBody ContrapropostaRequestDTO dto) {
        Proposta contra = propostaService.criarContraproposta(id, dto.descricao(), dto.usuarioId(), dto.itemId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PropostaResponseDTO.fromEntity(contra));
    }

    // ---------------- transições (State) ----------------

    /** Raiz: Rascunho → Homologação. Contraproposta: Rascunho → Em Análise. */
    @PatchMapping("/{id}/submeter")
    public PropostaResponseDTO submeter(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.submeter(id));
    }

    /** Raiz: Homologação → Ativa. */
    @PatchMapping("/{id}/aprovar")
    public PropostaResponseDTO aprovar(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.aprovar(id));
    }

    /** Volta para edição (Rascunho). */
    @PatchMapping("/{id}/editar")
    public PropostaResponseDTO voltarParaRascunho(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.voltarParaRascunho(id));
    }

    /** Contraproposta: o dono da raiz aceita. Contra e raiz vão para Negociado. */
    @PatchMapping("/{id}/aceitar")
    public PropostaResponseDTO aceitar(@PathVariable Long id, @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.aceitar(id, usuarioId));
    }

    /** Contraproposta em análise: o dono da raiz recusa. */
    @PatchMapping("/{id}/recusar")
    public PropostaResponseDTO recusar(@PathVariable Long id, @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.recusar(id, usuarioId));
    }

    /** Contraproposta em negociação: uma das partes desiste; a raiz volta para Ativa. */
    @PatchMapping("/{id}/desistir")
    public PropostaResponseDTO desistir(@PathVariable Long id, @RequestParam Long usuarioId) {
        return PropostaResponseDTO.fromEntity(propostaService.desistirDaNegociacao(id, usuarioId));
    }

    @PatchMapping("/{id}/cancelar")
    public PropostaResponseDTO cancelar(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.cancelar(id));
    }

    /**
     * Finaliza a negociação (id da raiz ou da contraproposta em negociação).
     * Uma linha aqui — todo o resto acontece por trás da Facade.
     */
    @PatchMapping("/{id}/finalizar")
    public PropostaResponseDTO finalizar(@PathVariable Long id) {
        return PropostaResponseDTO.fromEntity(propostaService.finalizarNegociacao(id));
    }

    private List<PropostaResponseDTO> paraDTO(List<Proposta> propostas) {
        return propostas.stream().map(PropostaResponseDTO::fromEntity).toList();
    }
}
