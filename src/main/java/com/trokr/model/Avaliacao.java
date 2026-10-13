package com.trokr.model;

import com.trokr.exception.AcessoNegadoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Avaliação de um lado da troca sobre o outro (Aula 7).
 *
 * Nasce PENDENTE, criada automaticamente pelo AtualizarAvaliacaoListener
 * quando a troca é concluída — sem nota nem descrição. Depois o autor
 * preenche via PATCH /avaliacoes/{id} e ela vira AVALIADA.
 */
@Entity
@Table(name = "avaliacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Avaliacao {

    public static final int NOTA_MINIMA = 1;
    public static final int NOTA_MAXIMA = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null enquanto PENDENTE. */
    private Integer nota;

    /** null enquanto PENDENTE. */
    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAvaliacao status = StatusAvaliacao.PENDENTE;

    /** A Proposta raiz da troca avaliada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    /** Quem avalia. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_autor_id", nullable = false)
    private Usuario usuarioAutor;

    /** Quem é avaliado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_alvo_id", nullable = false)
    private Usuario usuarioAlvo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAvaliacao;

    /** Cria uma avaliação PENDENTE: {@code autor} vai avaliar {@code alvo}. */
    public static Avaliacao pendente(Proposta proposta, Usuario autor, Usuario alvo) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.proposta = proposta;
        avaliacao.usuarioAutor = autor;
        avaliacao.usuarioAlvo = alvo;
        avaliacao.status = StatusAvaliacao.PENDENTE;
        return avaliacao;
    }

    /**
     * Preenche a avaliação. Regras: só uma vez (status PENDENTE), só pelo
     * próprio autor e com nota entre 1 e 5.
     */
    public void avaliar(Usuario quemAvalia, int nota, String descricao) {
        if (status != StatusAvaliacao.PENDENTE) {
            throw new IllegalStateException("Esta avaliação já foi preenchida");
        }
        if (!usuarioAutor.mesmoUsuario(quemAvalia)) {
            throw new AcessoNegadoException("Só o autor da avaliação pode preenchê-la — não é possível avaliar em nome de outra pessoa");
        }
        if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new IllegalArgumentException("A nota deve ser entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA);
        }
        this.nota = nota;
        this.descricao = descricao;
        this.status = StatusAvaliacao.AVALIADA;
        this.dataAvaliacao = LocalDateTime.now();
    }
}
