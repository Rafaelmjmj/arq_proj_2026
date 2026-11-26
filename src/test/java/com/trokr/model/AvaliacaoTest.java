package com.trokr.model;

import static com.trokr.model.Fixtures.item;
import static com.trokr.model.Fixtures.propostaAtiva;
import static com.trokr.model.Fixtures.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trokr.exception.AcessoNegadoException;
import com.trokr.exception.RegraNegocioException;
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
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> anaAvaliaBruno.avaliar(ana, 1, "Mudei de ideia")); // CT-A02: 422
        assertEquals("AVALIACAO_JA_REALIZADA", erro.getCodigo());
        assertEquals(5, anaAvaliaBruno.getNota()); // a nota continua 5
        assertTrue(erro.getMessage().contains("nota 5"));
        assertEquals(5, anaAvaliaBruno.getNota());
    }

    @Test
    void naoAvaliaEmNomeDeOutraPessoa() {
        assertThrows(AcessoNegadoException.class, () -> anaAvaliaBruno.avaliar(bruno, 5, "Eu sou ótimo"));
        assertEquals(StatusAvaliacao.PENDENTE, anaAvaliaBruno.getStatus());
    }

    @Test
    void notaForaDaFaixa() {
        assertThrows(RegraNegocioException.class, () -> anaAvaliaBruno.avaliar(ana, 6, "?"));
        assertThrows(RegraNegocioException.class, () -> anaAvaliaBruno.avaliar(ana, 0, "?"));
    }

    @Test
    void semInformarQuemAvaliaAChecagemDeAutoriaEhPulada() { // CT-A01 manda só nota e descrição
        anaAvaliaBruno.avaliar(null, 5, "Livro em ótimo estado");
        assertEquals(StatusAvaliacao.AVALIADA, anaAvaliaBruno.getStatus());
    }
}
