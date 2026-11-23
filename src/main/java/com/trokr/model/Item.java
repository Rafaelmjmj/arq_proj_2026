package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Algo que um usuário oferece numa troca. Aula 12: cada tipo de oferta tem
 * dados próprios, então Item virou ABSTRATO, com uma subclasse por tipo:
 * {@link ItemProduto} (pesoGramas), {@link ItemServico} (duracaoMinutos) e
 * {@link ItemExperiencia} (nivel).
 *
 * <p>Herança no JPA: SINGLE_TABLE. Uma tabela só ("item"), e a coluna
 * {@code categoria} — criada na Aula 9 — virou o DISCRIMINADOR: o JPA lê o
 * valor dela para decidir qual subclasse instanciar. Os itens que já estavam
 * no banco carregam como a subclasse certa, sem migração. O preço: as colunas
 * próprias de cada tipo aceitam nulo no banco; a garantia "todo produto tem
 * peso" passa para o código, na criação — é o trabalho das fábricas
 * (pacote {@code factory}).
 *
 * <p>Ninguém pergunta o tipo com instanceof (com carregamento LAZY, o
 * Hibernate entrega um proxy de Item, não de ItemProduto, e o instanceof
 * falha). Pergunta-se ao objeto: {@link #getCategoria()} e
 * {@link #dadosDoTipo()} são polimórficos.
 *
 * <p>Builder: {@code @SuperBuilder} em Item e em todas as subclasses, para
 * o builder de ItemProduto enxergar também titulo, descricao e dono. O
 * construtor vazio protegido é o que o JPA exige.
 *
 * Relação Item → Usuario unidirecional: o item conhece o dono, mas Usuario
 * não guarda a lista de itens.
 */
@Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "categoria", discriminatorType = DiscriminatorType.STRING)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioProprietario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /**
     * A categoria agora vem do TIPO do objeto: cada subclasse responde com a
     * sua constante. A Strategy de créditos (Aula 9) continua chamando este
     * método e não precisou mudar nenhuma linha.
     */
    public abstract CategoriaItem getCategoria();

    /** Os campos próprios deste tipo; os dos outros tipos vêm nulos. */
    public abstract DadosDoTipo dadosDoTipo();

    /** Cada subclasse copia só o campo que é seu. Chamado depois da fábrica validar. */
    protected abstract void aplicarDadosDoTipo(DadosDoTipo dados);

    /**
     * Atualiza os dados comuns e os do tipo. A categoria NÃO muda: um produto
     * não vira serviço (o catálogo de fábricas recusa antes de chegar aqui).
     */
    public void atualizar(String titulo, String descricao, Usuario proprietario, DadosDoTipo dados) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.usuarioProprietario = proprietario;
        aplicarDadosDoTipo(dados);
    }

    /** true se o item pertence ao usuário informado. */
    public boolean pertenceA(Usuario usuario) {
        return usuarioProprietario != null && usuarioProprietario.mesmoUsuario(usuario);
    }
}
