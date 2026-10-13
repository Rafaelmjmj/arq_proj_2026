package com.trokr.repository;

import com.trokr.model.Historico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoRepository extends JpaRepository<Historico, Long> {

    List<Historico> findByPropostaIdOrderByDataAsc(Long propostaId);
}
