package com.trokr.service;

import com.trokr.exception.ResourceNotFoundException;
import com.trokr.model.Avaliacao;
import com.trokr.model.Usuario;
import com.trokr.repository.AvaliacaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avaliações são criadas automaticamente (PENDENTE) pelo
 * AtualizarAvaliacaoListener. Aqui só se lista e se preenche.
 * As regras (status, autoria, faixa da nota) moram em Avaliacao.avaliar().
 */
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;

    public List<Avaliacao> listar(Long propostaId, Long usuarioAlvoId, Long usuarioAutorId) {
        if (propostaId != null) {
            return avaliacaoRepository.findByPropostaId(propostaId);
        }
        if (usuarioAlvoId != null) {
            return avaliacaoRepository.findByUsuarioAlvoId(usuarioAlvoId);
        }
        if (usuarioAutorId != null) {
            return avaliacaoRepository.findByUsuarioAutorId(usuarioAutorId);
        }
        return avaliacaoRepository.findAll();
    }

    public Avaliacao buscarPorId(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada com id " + id));
    }

    @Transactional
    public Avaliacao avaliar(Long id, Long usuarioId, int nota, String descricao) {
        Avaliacao avaliacao = buscarPorId(id);
        Usuario quemAvalia = usuarioService.buscarPorId(usuarioId);
        avaliacao.avaliar(quemAvalia, nota, descricao);
        return avaliacaoRepository.save(avaliacao);
    }
}
