package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Um usuário da plataforma: quem oferece e quem recebe nas trocas.
 *
 * Nesta fase do curso, Usuario ainda não tem senha, papel/role ou qualquer
 * conceito de autenticação — isso não faz parte do escopo até aqui.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String cidade;

    /**
     * Aula 9 — créditos ganhos em trocas concluídas.
     *
     * Sem setter de propósito: o saldo só muda por {@link #creditar(int)},
     * chamado pelo AtribuirCreditosListener quando uma troca é concluída.
     * Dado derivado de regra de negócio não se edita pela API.
     *
     * O "default 0" no banco evita erro do ddl-auto=update ao criar a coluna
     * NOT NULL numa tabela que já tem usuários cadastrados.
     */
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer saldoCreditos = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Usuario(String nome, String email, String cidade) {
        this.nome = nome;
        this.email = email;
        this.cidade = cidade;
    }

    /** Soma créditos ao saldo. Só aceita valores positivos. */
    public void creditar(int creditos) {
        if (creditos <= 0) {
            throw new IllegalArgumentException("Créditos devem ser positivos: " + creditos);
        }
        this.saldoCreditos = (saldoCreditos == null ? 0 : saldoCreditos) + creditos;
    }

    /** Compara pelo id — útil nas validações de autoria (quem pode aceitar, avaliar...). */
    public boolean mesmoUsuario(Usuario outro) {
        return outro != null && id != null && id.equals(outro.getId());
    }
}
