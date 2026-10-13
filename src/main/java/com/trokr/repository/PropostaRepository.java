package com.trokr.repository;

import com.trokr.model.Proposta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /** Todas as contrapropostas de uma proposta raiz (as "concorrentes"). */
    List<Proposta> findByPropostaPaiId(Long propostaPaiId);

    /** Só as propostas raiz (sem pai). */
    List<Proposta> findByPropostaPaiIsNull();

    List<Proposta> findByUsuarioId(Long usuarioId);
}
