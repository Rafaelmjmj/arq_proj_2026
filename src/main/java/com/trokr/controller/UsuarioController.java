package com.trokr.controller;

import com.trokr.dto.UsuarioRequestDTO;
import com.trokr.dto.UsuarioResponseDTO;
import com.trokr.model.Usuario;
import com.trokr.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listarTodos().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long id) {
        return UsuarioResponseDTO.fromEntity(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario(dto.nome(), dto.email(), dto.cidade());

        Usuario salvo = usuarioService.criar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario dadosAtualizados = new Usuario(dto.nome(), dto.email(), dto.cidade());

        return UsuarioResponseDTO.fromEntity(usuarioService.atualizar(id, dadosAtualizados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public UsuarioResponseDTO buscarPorEmail(@RequestParam("email") String email) {
        return UsuarioResponseDTO.fromEntity(usuarioService.buscarPorEmail(email));
    }
    @GetMapping("/buscar-nome")
    public List<UsuarioResponseDTO> buscarPorNome(@RequestParam("nome") String nome) {
        return usuarioService.listarPorNome(nome).stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }


}