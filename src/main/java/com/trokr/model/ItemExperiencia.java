package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Experiência (passeio de caiaque, jantar em casa). Pode ser para iniciantes
 * ou só para quem já pratica.
 *
 * Uma linha na tabela "item" com categoria = EXPERIENCIA. A coluna nivel aceita
 * nulo no banco (SINGLE_TABLE: as linhas dos outros tipos não a preenchem);
 * quem garante que toda experiência tenha nivel é a fábrica deste tipo.
 */
@Entity
@DiscriminatorValue("EXPERIENCIA")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemExperiencia extends Item {

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel")
    private NivelExperiencia nivel;

    @Override
    public CategoriaItem getCategoria() {
        return CategoriaItem.EXPERIENCIA;
    }

    @Override
    public DadosDoTipo dadosDoTipo() {
        return new DadosDoTipo(null, null, nivel);
    }

    @Override
    protected void aplicarDadosDoTipo(DadosDoTipo dados) {
        this.nivel = dados.nivel();
    }
}
