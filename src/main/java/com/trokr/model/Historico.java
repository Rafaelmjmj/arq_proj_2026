package com.trokr.model;

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

/**
 * Auditoria de negócio de UMA Proposta (Postgres): "esta proposta mudou de X
 * para Y, neste momento, por este motivo".
 *
 * Não confundir com LogEvento (Mongo, Aula 8): Historico responde "o que
 * aconteceu com ESTA proposta?"; LogEvento responde "o que está acontecendo
 * no SISTEMA?".
 */
@Entity
@Table(name = "historico")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Historico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    private Status statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status statusNovo;

    @Column(nullable = false)
    private LocalDateTime data;

    /** Quem causou a mudança — opcional (nem toda mudança tem um autor claro). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Historico(Proposta proposta, String motivo, Status statusAnterior, Status statusNovo,
                     LocalDateTime data, Usuario usuario) {
        this.proposta = proposta;
        this.motivo = motivo;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.data = data;
        this.usuario = usuario;
    }
}
