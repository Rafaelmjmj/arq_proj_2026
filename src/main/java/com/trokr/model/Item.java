package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Um item ou habilidade oferecido por um usuário para troca.
 *
 * Mantido como uma entidade genérica de propósito: nesta fase do curso, um
 * Item ainda não distingue entre produto físico, serviço ou aula — essa
 * especialização (e o padrão de projeto que vai resolvê-la) chega em aula
 * futura. Também não existe ainda o conceito de "Proposta de Troca" ligando
 * dois itens: cada Item apenas existe e pertence a um usuário por enquanto.
 */
@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;
    


    // Relação unidirecional de propósito: Item conhece seu dono, mas Usuario
    // não mantém uma coleção de itens. Evita decisões de cascade/fetch que
    // ainda não fazem sentido discutir nesta fase do curso.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioProprietario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
}
