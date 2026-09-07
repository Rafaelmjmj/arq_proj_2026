package com.trokr.repository;
import java.util.List;
import java.util.Optional;
import com.trokr.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// Uso direto do Spring Data JPA, sem interface/abstração genérica de
// repositório por cima — não há necessidade disso ainda.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByNomeContaining(String trecho);
    List<Usuario> findTop5ByOrderByDataCriacaoDesc();
    boolean existsByEmail(String Email);
}
