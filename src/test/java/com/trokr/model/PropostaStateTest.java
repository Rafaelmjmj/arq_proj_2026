package com.trokr.model;

import static com.trokr.model.Fixtures.contrapropostaEmAnalise;
import static com.trokr.model.Fixtures.item;
import static com.trokr.model.Fixtures.propostaAtiva;
import static com.trokr.model.Fixtures.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trokr.exception.AcessoNegadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Domain Model rico + State Pattern testados como POJO puro: sem Spring,
 * sem banco. Cobre as duas máquinas, transições válidas e inválidas, o
 * efeito cascata e as validações de quem pode agir.
 */
class PropostaStateTest {

    private Usuario ana;
    private Usuario bruno;
    private Usuario carla;
    private Item bicicleta;
    private Item aulaViolao;
    private Item passeio;

    @BeforeEach
    void setUp() {
        ana = usuario(1, "Ana");
        bruno = usuario(2, "Bruno");
        carla = usuario(3, "Carla");
        bicicleta = item(10, "Bicicleta", CategoriaItem.PRODUTO, ana);
        aulaViolao = item(20, "Aula de violão", CategoriaItem.SERVICO, bruno);
        passeio = item(30, "Passeio guiado", CategoriaItem.EXPERIENCIA, carla);
    }

    // ---------------- máquina da raiz ----------------

    @Test
    void propostaNovaComecaEmRascunho() {
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        assertEquals(Status.RASCUNHO, proposta.getStatus());
        assertFalse(proposta.isContraProposta());
        assertTrue(proposta.isPodeMudar());
    }

    @Test
    void caminhoFelizDaRaizAteAtiva() {
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        proposta.submeter();
        assertEquals(Status.HOMOLOGACAO, proposta.getStatus());
        proposta.aprovar();
        assertEquals(Status.ATIVA, proposta.getStatus());
    }

    @Test
    void ativaPodeVoltarParaRascunhoParaEdicao() {
        Proposta proposta = propostaAtiva(ana, bicicleta);
        proposta.voltarParaRascunho();
        assertEquals(Status.RASCUNHO, proposta.getStatus());
    }

    @Test
    void naoAprovaPropostaAindaEmRascunho() {
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        assertThrows(IllegalStateException.class, proposta::aprovar);
        assertEquals(Status.RASCUNHO, proposta.getStatus());
    }

    @Test
    void naoFinalizaSemContrapropostaEmNegociacao() {
        Proposta proposta = propostaAtiva(ana, bicicleta);
        assertThrows(IllegalStateException.class, proposta::finalizar);
    }

    @Test
    void canceladaEhTerminal() {
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        proposta.cancelar();
        assertEquals(Status.CANCELADO_RAIZ, proposta.getStatus());
        assertFalse(proposta.isPodeMudar());
        assertThrows(IllegalStateException.class, proposta::submeter);
        assertThrows(IllegalStateException.class, proposta::cancelar);
    }

    @Test
    void naoOfereceItemDeOutraPessoa() {
        assertThrows(IllegalArgumentException.class,
                () -> new Proposta("Troco violão", "?", ana, aulaViolao));
    }

    // ---------------- máquina da contraproposta ----------------

    @Test
    void contrapropostaUsaSuaPropriaMaquina() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, raiz);

        assertTrue(contra.isContraProposta());
        assertEquals(Status.RASCUNHO, contra.getStatus());

        contra.submeter(); // na contraproposta, submeter leva a Em Análise (não Homologação)
        assertEquals(Status.EM_ANALISE, contra.getStatus());

        assertThrows(IllegalStateException.class, contra::aprovar); // aprovar é só da raiz
    }

    @Test
    void soPropostaAtivaRecebeContraproposta() {
        Proposta emRascunho = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        assertThrows(IllegalStateException.class,
                () -> Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, emRascunho));
    }

    @Test
    void naoContrapropoeAPropriaProposta() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Item outroDaAna = item(11, "Livro", CategoriaItem.PRODUTO, ana);
        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criarContraProposta("?", ana, outroDaAna, raiz));
    }

    @Test
    void aceitarLevaContraERaizParaNegociado() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);

        contra.aceitar(ana);

        assertEquals(Status.NEGOCIADO_CONTRA, contra.getStatus());
        assertEquals(Status.NEGOCIADO_RAIZ, raiz.getStatus());
    }

    @Test
    void soODonoDaRaizPodeAceitar() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);

        assertThrows(AcessoNegadoException.class, () -> contra.aceitar(bruno));
        assertThrows(AcessoNegadoException.class, () -> contra.aceitar(carla));
        assertEquals(Status.EM_ANALISE, contra.getStatus());
    }

    @Test
    void naoAceitaContrapropostaAindaEmRascunho() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, raiz);
        assertThrows(IllegalStateException.class, () -> contra.aceitar(ana));
        assertEquals(Status.ATIVA, raiz.getStatus());
    }

    @Test
    void naoAceitaDuasContrapropostasAoMesmoTempo() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);

        contraBruno.aceitar(ana);

        // a raiz já está em Negociado: a máquina da raiz barra a segunda aceitação
        assertThrows(IllegalStateException.class, () -> contraCarla.aceitar(ana));
    }

    @Test
    void donoRecusaContrapropostaEmAnalise() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);

        contra.recusar(ana);

        assertEquals(Status.RECUSADO, contra.getStatus());
        assertEquals(Status.ATIVA, raiz.getStatus());
        assertFalse(contra.isPodeMudar());
    }

    @Test
    void desistenciaDevolveRaizParaAtiva() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        contra.aceitar(ana);

        contra.desistirDaNegociacao(bruno);

        assertEquals(Status.RECUSADO, contra.getStatus());
        assertEquals(Status.ATIVA, raiz.getStatus()); // volta a receber contrapropostas
    }

    @Test
    void terceiroNaoPodeDesistirDaNegociacao() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        contra.aceitar(ana);

        assertThrows(AcessoNegadoException.class, () -> contra.desistirDaNegociacao(carla));
    }

    // ---------------- finalização + efeito cascata ----------------

    @Test
    void finalizarPelaRaizEncerraConcorrentes() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        Item livroDaCarla = item(31, "Livro", CategoriaItem.PRODUTO, carla);
        Proposta rascunhoCarla = Proposta.criarContraProposta("Ainda pensando", carla, livroDaCarla, raiz);

        contraBruno.aceitar(ana);
        raiz.finalizar(); // Cenário A: finaliza pela raiz

        assertEquals(Status.FINALIZADO, raiz.getStatus());
        assertEquals(Status.FINALIZADO_CONTRA, contraBruno.getStatus());
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus());
        assertEquals(Status.CANCELADO_CONTRA, rascunhoCarla.getStatus());
        assertTrue(raiz.isTrocaConcluida());
        assertEquals(contraBruno, raiz.getContrapropostaVencedora().orElseThrow());
    }

    @Test
    void finalizarPelaContrapropostaTemOMesmoEfeito() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);

        contraBruno.aceitar(ana);
        contraBruno.finalizar(); // Cenário B: finaliza pela contraproposta

        assertEquals(Status.FINALIZADO, raiz.getStatus());
        assertEquals(Status.FINALIZADO_CONTRA, contraBruno.getStatus());
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus());
        assertTrue(contraBruno.isTrocaConcluida());
    }

    @Test
    void naoFinalizaPelaContrapropostaQueNaoEstaNegociando() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        contraBruno.aceitar(ana);

        assertThrows(IllegalStateException.class, contraCarla::finalizar);
        assertEquals(Status.NEGOCIADO_RAIZ, raiz.getStatus());
    }

    @Test
    void cancelarRaizEncerraContrapropostas() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        contraBruno.aceitar(ana);

        raiz.cancelar();

        assertEquals(Status.CANCELADO_RAIZ, raiz.getStatus());
        assertEquals(Status.RECUSADO, contraBruno.getStatus());       // estava negociando
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus()); // estava em análise
    }

    @Test
    void finalizadaEhTerminal() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        contra.aceitar(ana);
        raiz.finalizar();

        assertFalse(raiz.isPodeMudar());
        assertFalse(contra.isPodeMudar());
        assertThrows(IllegalStateException.class, raiz::cancelar);
        assertThrows(IllegalStateException.class, raiz::finalizar);
    }
}
