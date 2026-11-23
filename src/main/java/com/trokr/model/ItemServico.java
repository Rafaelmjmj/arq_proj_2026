package com.trokr.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Serviço (aula de violão, revisão de texto). Uma aula de 60 minutos não é a
 * mesma troca que uma de 15.
 *
 * Uma linha na tabela "item" com categoria = SERVICO. A coluna duracao_minutos aceita
 * nulo no banco (SINGLE_TABLE: as linhas dos outros tipos não a preenchem);
 * quem garante que todo serviço tenha duracaoMinutos é a fábrica deste tipo.
 */
@Entity
@DiscriminatorValue("SERVICO")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemServico extends Item {

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    @Override
    public CategoriaItem getCategoria() {
        return CategoriaItem.SERVICO;
    }

    @Override
    public DadosDoTipo dadosDoTipo() {
        return new DadosDoTipo(null, duracaoMinutos, null);
    }

    @Override
    protected void aplicarDadosDoTipo(DadosDoTipo dados) {
        this.duracaoMinutos = dados.duracaoMinutos();
    }
}
