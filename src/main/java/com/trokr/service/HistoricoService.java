package com.trokr.service;

import com.trokr.model.Historico;
import com.trokr.repository.HistoricoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HistoricoRepository historicoRepository;

    public List<Historico> listarPorProposta(Long propostaId) {
        return historicoRepository.findByPropostaIdOrderByDataAsc(propostaId);
    }
}
