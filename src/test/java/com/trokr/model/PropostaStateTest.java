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
import com.trokr.exception.RegraNegocioException;
import com.trokr.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Domain Model rico + State Pattern testados como POJO puro: sem Spring,
 * sem banco. Cobre as duas máquinas, transições válidas e inválidas, o
 * efeito cascata e as validações de quem pode agir. Os estados e códigos
 * seguem o roteiro de testes do professor (CT-P, CT-C e CT-F).
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

    // ---------------- máquina da raiz (CT-P) ----------------

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
        proposta.enviarParaHomologacao();
        assertEquals(Status.HOMOLOGACAO, proposta.getStatus());
        proposta.aprovar();
        assertEquals(Status.ATIVA, proposta.getStatus());
    }

    @Test
    void idaEVoltaDaHomologacao() { // CT-P05
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        proposta.enviarParaHomologacao();
        proposta.recusarHomologacao();
        assertEquals(Status.RASCUNHO, proposta.getStatus());
        proposta.enviarParaHomologacao();
        assertEquals(Status.HOMOLOGACAO, proposta.getStatus());
    }

    @Test
    void soRecusaHomologacaoDeQuemEstaEmHomologacao() {
        Proposta ativa = propostaAtiva(ana, bicicleta);
        assertThrows(TransicaoInvalidaException.class, ativa::recusarHomologacao);
        assertEquals(Status.ATIVA, ativa.getStatus());
    }

    @Test
    void ativaPodeVoltarParaRascunhoParaEdicao() {
        Proposta proposta = propostaAtiva(ana, bicicleta);
        proposta.voltarParaRascunho();
        assertEquals(Status.RASCUNHO, proposta.getStatus());
    }

    @Test
    void naoAprovaPropostaAindaEmRascunho() { // CT-P04
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        TransicaoInvalidaException erro = assertThrows(TransicaoInvalidaException.class, proposta::aprovar);
        assertEquals(TransicaoInvalidaException.PROPOSTA, erro.getCodigo());
        assertEquals(Status.RASCUNHO, proposta.getStatus());
    }

    @Test
    void naoFinalizaRascunhoECitaOEstado() { // CT-P03
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        TransicaoInvalidaException erro = assertThrows(TransicaoInvalidaException.class, proposta::finalizar);
        assertEquals(TransicaoInvalidaException.PROPOSTA, erro.getCodigo());
        assertTrue(erro.getMessage().contains("RASCUNHO"));
    }

    @Test
    void naoFinalizaAtivaSemContrapropostaConfirmada() {
        Proposta proposta = propostaAtiva(ana, bicicleta);
        assertThrows(TransicaoInvalidaException.class, proposta::finalizar);
    }

    @Test
    void canceladaEhTerminal() {
        Proposta proposta = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        proposta.cancelar(null); // na raiz, a justificativa é opcional
        assertEquals(Status.CANCELADO_RAIZ, proposta.getStatus());
        assertFalse(proposta.isPodeMudar());
        assertThrows(TransicaoInvalidaException.class, proposta::enviarParaHomologacao);
        assertThrows(TransicaoInvalidaException.class, () -> proposta.cancelar("de novo"));
    }

    @Test
    void naoOfereceItemDeOutraPessoa() { // CT-P01
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> new Proposta("Troco violão", "?", ana, aulaViolao));
        assertEquals("ITEM_DE_OUTRO_USUARIO", erro.getCodigo());
    }

    @Test
    void transicaoInvalidaCarregaOCodigoDaMaquinaCerta() {
        Proposta raiz = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        TransicaoInvalidaException daRaiz = assertThrows(TransicaoInvalidaException.class, raiz::aprovar);
        assertEquals(TransicaoInvalidaException.PROPOSTA, daRaiz.getCodigo());

        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, propostaAtiva(ana, bicicleta));
        TransicaoInvalidaException daContra = assertThrows(TransicaoInvalidaException.class, () -> contra.aceitar(ana));
        assertEquals(TransicaoInvalidaException.CONTRAPROPOSTA, daContra.getCodigo());
    }

    // ---------------- máquina da contraproposta (CT-C) ----------------

    @Test
    void contrapropostaUsaSuaPropriaMaquina() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, raiz);

        assertTrue(contra.isContraProposta());
        assertEquals(Status.RASCUNHO, contra.getStatus());

        contra.enviarParaAnalise(); // na contraproposta, enviar leva a Em Análise (não Homologação)
        assertEquals(Status.EM_ANALISE, contra.getStatus());
    }

    @Test
    void acaoDaMaquinaErradaEh409() { // CT-C04
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, raiz);

        TransicaoInvalidaException aprovar = assertThrows(TransicaoInvalidaException.class, contra::aprovar);
        assertEquals(TransicaoInvalidaException.PROPOSTA, aprovar.getCodigo());
        assertThrows(TransicaoInvalidaException.class, contra::enviarParaHomologacao);
        assertThrows(TransicaoInvalidaException.class, raiz::enviarParaAnalise);
        assertThrows(TransicaoInvalidaException.class, () -> raiz.aceitar(ana));
    }

    @Test
    void soPropostaAtivaRecebeContraproposta() {
        Proposta emRascunho = new Proposta("Troco bicicleta", "Aro 29", ana, bicicleta);
        assertThrows(TransicaoInvalidaException.class,
                () -> Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, emRascunho));
    }

    @Test
    void naoContrapropoeAPropriaProposta() { // CT-C01
        Proposta raiz = propostaAtiva(ana, bicicleta);
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> Proposta.criarContraProposta("?", ana, bicicleta, raiz));
        assertEquals("CONTRAPROPOSTA_NA_PROPRIA_PROPOSTA", erro.getCodigo());
    }

    @Test
    void aceiteEmDuasEtapas() { // CT-C07
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);

        contra.aceitar(null);
        assertEquals(Status.NEGOCIADO_CONTRA, contra.getStatus());
        assertEquals(Status.ATIVA, raiz.getStatus()); // a raiz só muda na confirmação

        raiz.aceitarContraproposta(null);
        assertEquals(Status.NEGOCIADO_RAIZ, raiz.getStatus());
    }

    @Test
    void naoConfirmaSemContrapropostaAceita() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        assertThrows(TransicaoInvalidaException.class, () -> raiz.aceitarContraproposta(null));
        assertEquals(Status.ATIVA, raiz.getStatus());
    }

    @Test
    void quemAgeSeInformadoPrecisaSerODonoDaRaiz() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);

        assertThrows(AcessoNegadoException.class, () -> contra.aceitar(bruno));
        assertThrows(AcessoNegadoException.class, () -> contra.aceitar(carla));
        assertEquals(Status.EM_ANALISE, contra.getStatus());

        contra.aceitar(ana);
        assertThrows(AcessoNegadoException.class, () -> raiz.aceitarContraproposta(bruno));
    }

    @Test
    void naoAceitaContrapropostaAindaEmRascunho() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = Proposta.criarContraProposta("Ofereço aula", bruno, aulaViolao, raiz);
        assertThrows(TransicaoInvalidaException.class, () -> contra.aceitar(ana));
        assertEquals(Status.ATIVA, raiz.getStatus());
    }

    @Test
    void naoAceitaDuasContrapropostasAoMesmoTempo() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);

        contraBruno.aceitar(ana);

        assertThrows(TransicaoInvalidaException.class, () -> contraCarla.aceitar(ana));
        assertEquals(Status.EM_ANALISE, contraCarla.getStatus());
    }

    @Test
    void cancelarContrapropostaExigeJustificativa() { // CT-C06
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, carla, passeio);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> contra.cancelar(""));
        assertEquals("JUSTIFICATIVA_OBRIGATORIA", erro.getCodigo());
        assertEquals(Status.EM_ANALISE, contra.getStatus());

        contra.cancelar("Encontrei outra troca");
        assertEquals(Status.CANCELADO_CONTRA, contra.getStatus());
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
        raiz.aceitarContraproposta(ana);

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

    // ---------------- finalização + efeito cascata (CT-F) ----------------

    private void negociar(Proposta raiz, Proposta contra) {
        contra.aceitar(null);
        raiz.aceitarContraproposta(null);
    }

    @Test
    void finalizarPelaRaizEncerraConcorrentesEPreservaAVencedora() { // CT-F01, CT-F02
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        Item livroDaCarla = item(31, "Livro", CategoriaItem.PRODUTO, carla);
        Proposta rascunhoCarla = Proposta.criarContraProposta("Ainda pensando", carla, livroDaCarla, raiz);

        negociar(raiz, contraBruno);
        raiz.finalizar();

        assertEquals(Status.FINALIZADA, raiz.getStatus());
        assertEquals(Status.NEGOCIADO_CONTRA, contraBruno.getStatus()); // a vencedora não é cancelada
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus());
        assertEquals(Status.CANCELADO_CONTRA, rascunhoCarla.getStatus());
        assertTrue(raiz.isTrocaConcluida());
        assertEquals(contraBruno, raiz.getContrapropostaVencedora().orElseThrow());
    }

    @Test
    void finalizarPelaContrapropostaAceitaTemOMesmoEfeito() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);

        negociar(raiz, contraBruno);
        contraBruno.finalizar();

        assertEquals(Status.FINALIZADA, raiz.getStatus());
        assertEquals(Status.NEGOCIADO_CONTRA, contraBruno.getStatus());
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus());
        assertTrue(contraBruno.isTrocaConcluida());
    }

    @Test
    void naoFinalizaPelaContrapropostaQueNaoFoiAceita() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        negociar(raiz, contraBruno);

        assertThrows(TransicaoInvalidaException.class, contraCarla::finalizar);
        assertEquals(Status.NEGOCIADO_RAIZ, raiz.getStatus());
    }

    @Test
    void naoFinalizaSoComAContrapropostaAceitaSemConfirmacao() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        contra.aceitar(null); // falta a 2ª etapa

        assertThrows(TransicaoInvalidaException.class, raiz::finalizar);
        assertEquals(Status.ATIVA, raiz.getStatus());
    }

    @Test
    void cancelarRaizEncerraContrapropostas() {
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contraBruno = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        Proposta contraCarla = contrapropostaEmAnalise(raiz, carla, passeio);
        negociar(raiz, contraBruno);

        raiz.cancelar(null);

        assertEquals(Status.CANCELADO_RAIZ, raiz.getStatus());
        assertEquals(Status.RECUSADO, contraBruno.getStatus());       // estava negociando
        assertEquals(Status.CANCELADO_CONTRA, contraCarla.getStatus()); // estava em análise
    }

    @Test
    void finalizadaEhTerminalEFinalizarDeNovoCitaOEstado() { // CT-F06
        Proposta raiz = propostaAtiva(ana, bicicleta);
        Proposta contra = contrapropostaEmAnalise(raiz, bruno, aulaViolao);
        negociar(raiz, contra);
        raiz.finalizar();

        assertFalse(raiz.isPodeMudar());
        assertThrows(TransicaoInvalidaException.class, () -> raiz.cancelar(null));
        TransicaoInvalidaException erro = assertThrows(TransicaoInvalidaException.class, raiz::finalizar);
        assertEquals(TransicaoInvalidaException.PROPOSTA, erro.getCodigo());
        assertTrue(erro.getMessage().contains("FINALIZADA"));
        assertThrows(TransicaoInvalidaException.class, () -> contra.desistirDaNegociacao(bruno));
    }
}
