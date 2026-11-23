package com.trokr.controller;

import com.trokr.dto.UsuarioRequestDTO;
import com.trokr.dto.UsuarioResponseDTO;
import com.trokr.model.Usuario;
import com.trokr.service.UsuarioService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuários", description = "Cadastro e consulta de quem participa das trocas. "
        + "O saldo de créditos só muda quando uma troca é finalizada.")
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Lista todos os usuários")
    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listarTodos().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @Operation(summary = "Busca um usuário pelo id",
            description = "Se o id não existir, responde 404 com um ProblemDetail.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "USUARIO_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(
            @Parameter(description = "Id do usuário", example = "1") @PathVariable Long id) {
        return UsuarioResponseDTO.fromEntity(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Cadastra um usuário",
            description = "Começa com saldo zero. O e-mail é único.")
    @ApiResponse(responseCode = "201", description = "Usuário criado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "E-mail já usado por outro usuário (EMAIL_JA_CADASTRADO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario(dto.nome(), dto.email(), dto.cidade());

        Usuario salvo = usuarioService.criar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.fromEntity(salvo));
    }

    @Operation(summary = "Atualiza nome, e-mail e cidade de um usuário")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "USUARIO_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "E-mail já usado por outro usuário (EMAIL_JA_CADASTRADO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizar(
            @Parameter(description = "Id do usuário", example = "1") @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario dadosAtualizados = new Usuario(dto.nome(), dto.email(), dto.cidade());

        return UsuarioResponseDTO.fromEntity(usuarioService.atualizar(id, dadosAtualizados));
    }

    @Operation(summary = "Remove um usuário")
    @ApiResponse(responseCode = "204", description = "Usuário removido")
    @ApiResponse(responseCode = "404", description = "USUARIO_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O usuário ainda tem itens ou propostas (RESTRICAO_DE_INTEGRIDADE)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @Parameter(description = "Id do usuário", example = "1") @PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca um usuário pelo e-mail exato")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "USUARIO_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/buscar")
    public UsuarioResponseDTO buscarPorEmail(
            @Parameter(description = "E-mail completo", example = "ana@trokr.dev") @RequestParam("email") String email) {
        return UsuarioResponseDTO.fromEntity(usuarioService.buscarPorEmail(email));
    }
    @Operation(summary = "Lista usuários cujo nome contém o texto",
            description = "Sem diferenciar maiúsculas de minúsculas.")
    @GetMapping("/buscar-nome")
    public List<UsuarioResponseDTO> buscarPorNome(
            @Parameter(description = "Parte do nome", example = "an") @RequestParam("nome") String nome) {
        return usuarioService.listarPorNome(nome).stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }


}