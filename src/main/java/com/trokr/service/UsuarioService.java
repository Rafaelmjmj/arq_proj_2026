package com.trokr.service;

import com.trokr.exception.ResourceNotFoundException;
import com.trokr.model.Usuario;
import com.trokr.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Classe concreta, sem interface própria: só existe uma implementação, então
// uma interface aqui só adicionaria indireção sem benefício real.
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarPorNome(String nome) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario não encontrado com id " + id));
    }
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
    }

    public Usuario criar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, Usuario dadosAtualizados) {
        Usuario usuarioExistente = buscarPorId(id);
        usuarioExistente.setNome(dadosAtualizados.getNome());
        usuarioExistente.setEmail(dadosAtualizados.getEmail());
        usuarioExistente.setCidade(dadosAtualizados.getCidade());
        return usuarioRepository.save(usuarioExistente);
    }

    public void remover(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

}