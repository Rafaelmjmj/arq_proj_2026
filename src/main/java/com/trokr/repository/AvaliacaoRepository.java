package com.trokr.repository;

import com.trokr.model.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByPropostaId(Long propostaId);

    List<Avaliacao> findByUsuarioAlvoId(Long usuarioAlvoId);

    List<Avaliacao> findByUsuarioAutorId(Long usuarioAutorId);
}
