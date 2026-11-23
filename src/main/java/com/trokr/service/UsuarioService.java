package com.trokr.service;

import com.trokr.exception.RecursoNaoEncontradoException;
import com.trokr.exception.RegraNegocioException;
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
                .orElseThrow(() -> RecursoNaoEncontradoException.usuario(id));
    }
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO",
                        "Usuário não encontrado com email " + email));
    }

    public Usuario criar(Usuario usuario) {
        exigirEmailLivre(usuario.getEmail(), null);
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, Usuario dadosAtualizados) {
        Usuario usuarioExistente = buscarPorId(id);
        exigirEmailLivre(dadosAtualizados.getEmail(), id);
        usuarioExistente.setNome(dadosAtualizados.getNome());
        usuarioExistente.setEmail(dadosAtualizados.getEmail());
        usuarioExistente.setCidade(dadosAtualizados.getCidade());
        return usuarioRepository.save(usuarioExistente);
    }

    public void remover(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    /**
     * E-mail duplicado é regra de negócio (422, EMAIL_JA_CADASTRADO), não um
     * erro de banco: conferimos antes de gravar, em vez de deixar a
     * constraint UNIQUE estourar como DataIntegrityViolationException.
     */
    private void exigirEmailLivre(String email, Long idAtual) {
        usuarioRepository.findByEmail(email)
                .filter(outro -> !outro.getId().equals(idAtual))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("EMAIL_JA_CADASTRADO", "Já existe um usuário com o e-mail " + email);
                });
    }
}
