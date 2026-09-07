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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
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
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());

        Usuario salvo = usuarioService.criar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario dadosAtualizados = new Usuario();
        dadosAtualizados.setNome(dto.nome());
        dadosAtualizados.setEmail(dto.email());

        return UsuarioResponseDTO.fromEntity(usuarioService.atualizar(id, dadosAtualizados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/email")
    public UsuarioResponseDTO buscarPorEmail(@RequestParam String email){
        return UsuarioResponseDTO.fromEntity(
            usuarioService.buscarPorEmail(email)
            );
    }

    @GetMapping("/buscar")
    public List<UsuarioResponseDTO> buscarPorNome(@RequestParam String nome){
        return usuarioService.buscarPorNome(nome).stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/recentes")
    public List<UsuarioResponseDTO> listarCincoMaisRecentes() {
    return usuarioService.listarCincoMaisRecentes().stream()
            .map(UsuarioResponseDTO::fromEntity)
            .toList();
    }
    @GetMapping("/existe")
    public boolean existePorEmail(@RequestParam String email) {
         return usuarioService.existePorEmail(email);
}   
}