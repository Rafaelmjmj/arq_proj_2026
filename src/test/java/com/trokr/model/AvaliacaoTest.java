package com.trokr.model;

import static com.trokr.model.Fixtures.item;
import static com.trokr.model.Fixtures.propostaAtiva;
import static com.trokr.model.Fixtures.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trokr.exception.AcessoNegadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvaliacaoTest {

    private Usuario ana;
    private Usuario bruno;
    private Avaliacao anaAvaliaBruno;

    @BeforeEach
    void setUp() {
        ana = usuario(1, "Ana");
        bruno = usuario(2, "Bruno");
        Proposta proposta = propostaAtiva(ana, item(10, "Bicicleta", CategoriaItem.PRODUTO, ana));
        anaAvaliaBruno = Avaliacao.pendente(proposta, ana, bruno);
    }

    @Test
    void nascePendenteSemNota() {
        assertEquals(StatusAvaliacao.PENDENTE, anaAvaliaBruno.getStatus());
        assertNull(anaAvaliaBruno.getNota());
        assertNull(anaAvaliaBruno.getDescricao());
    }

    @Test
    void autorPreencheEElaViraAvaliada() {
        anaAvaliaBruno.avaliar(ana, 5, "Troca tranquila");
        assertEquals(StatusAvaliacao.AVALIADA, anaAvaliaBruno.getStatus());
        assertEquals(5, anaAvaliaBruno.getNota());
    }

    @Test
    void naoAvaliaDuasVezes() {
        anaAvaliaBruno.avaliar(ana, 5, "Ótimo");
        assertThrows(IllegalStateException.class, () -> anaAvaliaBruno.avaliar(ana, 1, "Mudei de ideia"));
        assertEquals(5, anaAvaliaBruno.getNota());
    }

    @Test
    void naoAvaliaEmNomeDeOutraPessoa() {
        assertThrows(AcessoNegadoException.class, () -> anaAvaliaBruno.avaliar(bruno, 5, "Eu sou ótimo"));
        assertEquals(StatusAvaliacao.PENDENTE, anaAvaliaBruno.getStatus());
    }

    @Test
    void notaForaDaFaixa() {
        assertThrows(IllegalArgumentException.class, () -> anaAvaliaBruno.avaliar(ana, 6, "?"));
        assertThrows(IllegalArgumentException.class, () -> anaAvaliaBruno.avaliar(ana, 0, "?"));
    }
}
