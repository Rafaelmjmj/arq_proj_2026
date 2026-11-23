package com.trokr.repository;

import com.trokr.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Uso direto do Spring Data JPA, sem interface/abstração genérica de
// repositório por cima — não há necessidade disso ainda.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByNomeContainingIgnoreCase(String nome);

    /**
     * Aula 10, Corrida 2 — UPDATE atômico. "Somar ao saldo" é aritmética pura
     * e cabe num único comando: o Java não lê o saldo para depois gravar, então
     * não existe a janela do lost update (30 + 10 + 15 = 45 em vez de 55). Duas
     * trocas do mesmo usuário ao mesmo tempo: o segundo UPDATE espera o lock da
     * linha e soma sobre o valor já commitado. As duas dão 200.
     *
     * Devolve as linhas afetadas: 1 = creditado; 0 = usuário inexistente.
     *
     * flushAutomatically: grava antes as mudanças pendentes da transação (a
     * Proposta finalizada), na ordem certa. NÃO usamos clearAutomatically:
     * limpar o contexto no meio da finalização desanexaria a Proposta e os
     * usuários do evento que os outros listeners ainda vão ler. Em troca, a
     * regra é: depois deste UPDATE, ninguém salva o mesmo Usuario nesta
     * transação (o objeto Java ficou com o saldo antigo).
     */
    @Modifying(flushAutomatically = true)
    @Query("update Usuario u set u.saldoCreditos = u.saldoCreditos + :valor where u.id = :id")
    int adicionarCreditos(@Param("id") Long id, @Param("valor") int valor);
}
