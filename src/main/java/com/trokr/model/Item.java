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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Algo que um usuário oferece numa troca: um produto, um serviço ou uma
 * experiência. Cada Proposta aponta para o item que o autor está oferecendo.
 *
 * Relação Item → Usuario unidirecional: o item conhece o dono, mas Usuario
 * não guarda a lista de itens.
 */
@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    /** Aula 9 — define qual estratégia de crédito se aplica a este item. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaItem categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioProprietario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Item(String titulo, String descricao, CategoriaItem categoria, Usuario usuarioProprietario) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.usuarioProprietario = usuarioProprietario;
    }

    /** true se o item pertence ao usuário informado. */
    public boolean pertenceA(Usuario usuario) {
        return usuarioProprietario != null && usuarioProprietario.mesmoUsuario(usuario);
    }
}
