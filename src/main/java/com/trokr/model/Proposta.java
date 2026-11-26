package com.trokr.model;

import com.trokr.exception.AcessoNegadoException;
import com.trokr.exception.RegraNegocioException;
import com.trokr.exception.TransicaoInvalidaException;
import com.trokr.model.state.FabricaDeEstados;
import com.trokr.model.state.contraproposta.EstadoContraproposta;
import com.trokr.model.state.proposta.EstadoProposta;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Proposta de troca — Domain Model RICO + contexto do State Pattern.
 *
 * <p>Proposta e Contraproposta são a MESMA entidade: uma contraproposta é só
 * uma Proposta com {@code propostaPai} preenchido (autorrelacionamento).
 * Cada uma segue a sua própria máquina de estados (Opção A — duas interfaces):
 * <ul>
 *   <li>raiz: {@link EstadoProposta} — Rascunho, Homologação, Ativa,
 *       Negociado, Finalizado, Cancelado;</li>
 *   <li>contraproposta: {@link EstadoContraproposta} — Rascunho, Em Análise,
 *       Negociado, Finalizado, Recusado, Cancelado.</li>
 * </ul>
 *
 * <p>Não existe {@code setStatus()}: o status só muda pelos métodos de
 * negócio abaixo (submeter, aprovar, aceitar...), que DELEGAM para o estado
 * atual. É o estado quem decide se a transição é válida e chama
 * {@link #mudarEstadoPara}. O Service não tem nenhum if de transição.
 */
@Entity
@Table(name = "proposta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigido pelo JPA
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RASCUNHO;

    /** Autor: quem criou esta proposta (ou contraproposta). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** O que o autor está oferecendo nesta proposta. Precisa ser dele. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_oferecido_id", nullable = false)
    private Item itemOferecido;

    /** Autorrelacionamento: preenchido = é contraproposta; null = é raiz. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_pai_id")
    private Proposta propostaPai;

    /** Todas as contrapropostas recebidas por esta proposta raiz. */
    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "propostaPai")
    private List<Proposta> contrapropostas = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /**
     * Aula 10, Corrida 1 — lock otimista. O Hibernate incrementa este número a
     * cada UPDATE e o usa como condição ("... where id = ? and versao = ?").
     * Se duas requisições finalizarem a mesma Proposta ao mesmo tempo, a
     * segunda grava com uma versão que já não existe, afeta 0 linhas e recebe
     * ObjectOptimisticLockingFailureException: a transação dela inteira é
     * desfeita e o cliente recebe 409.
     *
     * Sem setter e fora dos DTOs: só o Hibernate escreve aqui. O campo fica
     * sem valor inicial de propósito: com o wrapper nulo, o Spring Data
     * reconhece a Proposta nova e faz persist (o Hibernate grava 0); o
     * "default 0" na coluna cobre as linhas que já existiam no banco.
     */
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long versao;

    // O objeto de estado não vai para o banco — só o enum "status" vai.
    // É reconstruído sob demanda a partir do status (ver FabricaDeEstados).
    @Transient
    @Getter(AccessLevel.NONE)
    private EstadoProposta estadoRaiz;

    @Transient
    @Getter(AccessLevel.NONE)
    private EstadoContraproposta estadoContra;

    // =====================================================================
    // Criação
    // =====================================================================

    /** Cria uma Proposta RAIZ, em Rascunho. */
    public Proposta(String titulo, String descricao, Usuario autor, Item itemOferecido) {
        if (!itemOferecido.pertenceA(autor)) {
            throw new RegraNegocioException("ITEM_DE_OUTRO_USUARIO", "O item " + itemOferecido.getId()
                    + " não pertence ao usuário " + autor.getId() + " — só é possível oferecer itens próprios");
        }
        this.titulo = titulo;
        this.descricao = descricao;
        this.usuario = autor;
        this.itemOferecido = itemOferecido;
        this.status = Status.RASCUNHO;
    }

    /**
     * Cria uma CONTRAPROPOSTA para {@code pai}, em Rascunho. As regras de
     * quem pode contrapropor moram aqui, na entidade, e não no Service.
     */
    public static Proposta criarContraProposta(String descricao, Usuario autor, Item itemOferecido, Proposta pai) {
        if (pai.isContraProposta()) {
            throw new RegraNegocioException("CONTRAPROPOSTA_DE_CONTRAPROPOSTA",
                    "Não é possível contrapropor uma contraproposta — responda à proposta original");
        }
        if (pai.getUsuario().mesmoUsuario(autor)) {
            throw new RegraNegocioException("CONTRAPROPOSTA_NA_PROPRIA_PROPOSTA",
                    "O autor não pode fazer contraproposta à própria proposta");
        }
        if (!pai.estadoRaiz().podeReceberContraproposta()) {
            throw new TransicaoInvalidaException("PROPOSTA_NAO_RECEBE_CONTRAPROPOSTA",
                    "Só propostas Ativas recebem contrapropostas (status atual: " + pai.getStatus() + ")");
        }
        Proposta contra = new Proposta(pai.getTitulo(), descricao, autor, itemOferecido);
        contra.propostaPai = pai;
        pai.adicionarContraproposta(contra);
        return contra;
    }

    // =====================================================================
    // State Pattern — chamado SOMENTE pelas classes de estado
    // =====================================================================

    public void mudarEstadoPara(EstadoProposta novoEstado) {
        if (isContraProposta()) {
            // Defeito de programação (nenhum fluxo do domínio chega aqui): fica
            // IllegalStateException de propósito e vira 500, não 409.
            throw new IllegalStateException("Contraproposta não usa a máquina de estados da raiz");
        }
        this.estadoRaiz = novoEstado;
        this.status = novoEstado.getStatus();
    }

    public void mudarEstadoPara(EstadoContraproposta novoEstado) {
        if (!isContraProposta()) {
            throw new IllegalStateException("Proposta raiz não usa a máquina de estados da contraproposta");
        }
        this.estadoContra = novoEstado;
        this.status = novoEstado.getStatus();
    }

    // =====================================================================
    // Ações de negócio — todas delegam para o estado atual
    // =====================================================================

    /** Raiz: Rascunho → Homologação. Contraproposta: Rascunho → Em Análise. */
    public void submeter() {
        if (isContraProposta()) {
            estadoContra().irParaEmAnalise(this);
        } else {
            estadoRaiz().irParaHomologacao(this);
        }
    }

    /** Só raiz: Rascunho → Homologação. */
    public void enviarParaHomologacao() {
        exigirRaiz("ser enviada para homologação");
        estadoRaiz().irParaHomologacao(this);
    }

    /** Só contraproposta: Rascunho → Em Análise. */
    public void enviarParaAnalise() {
        exigirContraproposta("ser enviada para análise");
        estadoContra().irParaEmAnalise(this);
    }

    /** Só raiz: Homologação → Ativa (aprovada pela plataforma). */
    public void aprovar() {
        exigirRaiz("aprovar");
        estadoRaiz().irParaAtiva(this);
    }

    /** Volta para edição. Raiz: Homologação/Ativa → Rascunho. Contra: Em Análise → Rascunho. */
    public void voltarParaRascunho() {
        if (isContraProposta()) {
            estadoContra().irParaRascunho(this);
        } else {
            estadoRaiz().irParaRascunho(this);
        }
    }

    /** Só raiz: a plataforma recusa a homologação. Homologação → Rascunho (e só a partir dela). */
    public void recusarHomologacao() {
        exigirRaiz("ter a homologação recusada");
        if (status != Status.HOMOLOGACAO) {
            throw TransicaoInvalidaException.daProposta(
                    "Só é possível recusar a homologação de uma proposta em HOMOLOGACAO (estado atual: " + status + ")");
        }
        estadoRaiz().irParaRascunho(this);
    }

    /**
     * Aceite em duas etapas, como no roteiro de referência. 1ª etapa, só
     * contraproposta: Em Análise → NEGOCIADO_CONTRA. A raiz ainda fica Ativa até
     * o dono confirmar com {@link #aceitarContraproposta}. Só uma
     * contraproposta por vez pode estar aceita.
     *
     * @param quemAceita opcional: se informado, precisa ser o dono da raiz (403)
     */
    public void aceitar(Usuario quemAceita) {
        exigirContraproposta("aceitar");
        if (quemAceita != null) {
            exigirDonoDaRaiz(quemAceita, "aceitar");
        }
        if (propostaPai.getStatus() != Status.ATIVA) {
            throw TransicaoInvalidaException.daContraproposta(
                    "Só é possível aceitar contrapropostas de uma proposta ATIVA (estado atual: " + propostaPai.getStatus() + ")");
        }
        if (propostaPai.getContrapropostaEmNegociacao().isPresent()) {
            throw TransicaoInvalidaException.daContraproposta(
                    "A proposta " + propostaPai.getId() + " já tem uma contraproposta aceita");
        }
        estadoContra().irParaNegociado(this);
    }

    /**
     * 2ª etapa, só raiz: o dono confirma a contraproposta aceita.
     * Ativa → NEGOCIADO_RAIZ. Sem contraproposta aceita, é ação fora de hora (409).
     *
     * @param quemAceita opcional: se informado, precisa ser o dono da raiz (403)
     */
    public void aceitarContraproposta(Usuario quemAceita) {
        exigirRaiz("aceitar contraproposta");
        if (quemAceita != null && !usuario.mesmoUsuario(quemAceita)) {
            throw new AcessoNegadoException("PROPOSTA_ACAO_NAO_AUTORIZADA",
                    "Só o dono da proposta pode aceitar uma contraproposta");
        }
        if (getContrapropostaEmNegociacao().isEmpty()) {
            throw TransicaoInvalidaException.daProposta(
                    "Não há contraproposta aceita para confirmar (estado atual: " + status + ")");
        }
        estadoRaiz().irParaNegociado(this);
    }

    /** Só contraproposta: o dono da Proposta original recusa. Em Análise → Recusado. */
    public void recusar(Usuario quemRecusa) {
        exigirContraproposta("recusar");
        if (quemRecusa != null) {
            exigirDonoDaRaiz(quemRecusa, "recusar");
        }
        estadoContra().irParaRecusado(this);
    }

    /**
     * Só contraproposta aceita: uma das duas partes desiste antes da
     * finalização. Contra: NEGOCIADO_CONTRA → Recusado; se a raiz já estava em
     * NEGOCIADO_RAIZ, volta para Ativa (volta a receber contrapropostas).
     */
    public void desistirDaNegociacao(Usuario quemDesiste) {
        exigirContraproposta("desistir da negociação");
        if (!usuario.mesmoUsuario(quemDesiste) && !propostaPai.getUsuario().mesmoUsuario(quemDesiste)) {
            throw new AcessoNegadoException("PROPOSTA_ACAO_NAO_AUTORIZADA", "Só as duas partes da negociação podem desistir dela");
        }
        if (propostaPai.getStatus() == Status.FINALIZADA) {
            throw TransicaoInvalidaException.daContraproposta("A troca já foi FINALIZADA: não é possível desistir");
        }
        if (status != Status.NEGOCIADO_CONTRA) {
            throw TransicaoInvalidaException.daContraproposta(
                    "Só é possível desistir de uma contraproposta aceita (estado atual: " + status + ")");
        }
        estadoContra().irParaRecusado(this);
        if (propostaPai.getStatus() == Status.NEGOCIADO_RAIZ) {
            propostaPai.estadoRaiz().irParaAtiva(propostaPai);
        }
    }

    /**
     * Cancela. Contraproposta: exige justificativa (422 JUSTIFICATIVA_OBRIGATORIA).
     * Raiz: justificativa opcional, e dispara o efeito cascata — as
     * contrapropostas ainda abertas são encerradas (cada estado decide como).
     */
    public void cancelar(String justificativa) {
        if (isContraProposta()) {
            if (justificativa == null || justificativa.isBlank()) {
                throw new RegraNegocioException("JUSTIFICATIVA_OBRIGATORIA",
                        "Para cancelar uma contraproposta, informe a justificativa");
            }
            estadoContra().irParaCancelado(this);
        } else {
            estadoRaiz().irParaCancelado(this);
            encerrarContrapropostas(null);
        }
    }

    /**
     * Finaliza a troca — chamado a partir da raiz (ou da contraproposta aceita,
     * que repassa para a raiz). A raiz precisa estar em NEGOCIADO_RAIZ; fora
     * disso, 409 citando o estado atual. Raiz → FINALIZADA; a contraproposta
     * vencedora continua em NEGOCIADO_CONTRA; as concorrentes ainda abertas são
     * encerradas (efeito cascata).
     */
    public void finalizar() {
        if (isContraProposta() && status != Status.NEGOCIADO_CONTRA) {
            throw TransicaoInvalidaException.daContraproposta(
                    "Só a contraproposta aceita finaliza a troca (estado atual: " + status + ")");
        }
        Proposta raiz = getRaiz();
        if (raiz.getStatus() != Status.NEGOCIADO_RAIZ) {
            throw TransicaoInvalidaException.daProposta(
                    "Não é possível finalizar uma proposta no estado " + raiz.getStatus());
        }
        Proposta vencedora = raiz.getContrapropostaEmNegociacao().orElseThrow(() ->
                TransicaoInvalidaException.daProposta("Nenhuma contraproposta aceita para finalizar"));
        raiz.estadoRaiz().irParaFinalizado(raiz);
        raiz.encerrarContrapropostas(vencedora);
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    public boolean isContraProposta() {
        return propostaPai != null;
    }

    /** A Proposta raiz desta negociação: ela mesma, ou a propostaPai. */
    public Proposta getRaiz() {
        return isContraProposta() ? propostaPai : this;
    }

    /** false se a proposta está num estado terminal. */
    public boolean isPodeMudar() {
        return isContraProposta() ? estadoContra().isPodeMudar() : estadoRaiz().isPodeMudar();
    }

    /** true quando a negociação chegou ao estado final de troca realizada. */
    public boolean isTrocaConcluida() {
        return getRaiz().getStatus() == Status.FINALIZADA;
    }

    public List<Proposta> getContrapropostas() {
        return Collections.unmodifiableList(contrapropostas);
    }

    /** A contraproposta que está em negociação agora (no máximo uma). */
    public Optional<Proposta> getContrapropostaEmNegociacao() {
        return buscarContrapropostaPorStatus(Status.NEGOCIADO_CONTRA);
    }

    /** A contraproposta que fechou a troca: a aceita, depois que a raiz foi FINALIZADA. */
    public Optional<Proposta> getContrapropostaVencedora() {
        return getRaiz().getStatus() == Status.FINALIZADA
                ? buscarContrapropostaPorStatus(Status.NEGOCIADO_CONTRA)
                : Optional.empty();
    }

    // =====================================================================
    // Internos
    //
    // "protected" e não "private" de propósito: propostaPai é carregada
    // LAZY, então pode ser um proxy do Hibernate. Métodos privados chamados
    // num proxy rodam no objeto vazio do proxy (campos nulos); métodos
    // protected são interceptados e repassados para a entidade real.
    // =====================================================================

    protected void adicionarContraproposta(Proposta contra) {
        contrapropostas.add(contra);
    }

    protected Optional<Proposta> buscarContrapropostaPorStatus(Status status) {
        return contrapropostas.stream().filter(c -> c.getStatus() == status).findFirst();
    }

    /** Efeito cascata: pede para cada contraproposta (menos a vencedora, se houver) se encerrar. */
    protected void encerrarContrapropostas(Proposta vencedora) {
        contrapropostas.stream()
                .filter(c -> c != vencedora)
                .forEach(c -> c.estadoContra().encerrarPorFimDaRaiz(c));
    }

    protected EstadoProposta estadoRaiz() {
        if (estadoRaiz == null) {
            estadoRaiz = FabricaDeEstados.estadoRaiz(status);
        }
        return estadoRaiz;
    }

    protected EstadoContraproposta estadoContra() {
        if (estadoContra == null) {
            estadoContra = FabricaDeEstados.estadoContraproposta(status);
        }
        return estadoContra;
    }

    /** Ação da máquina errada é ação fora de hora: 409, como no roteiro de referência (CT-C04). */
    private void exigirRaiz(String acao) {
        if (isContraProposta()) {
            throw TransicaoInvalidaException.daProposta("Só uma proposta raiz pode " + acao
                    + " — esta é uma contraproposta (estado " + status + ")");
        }
    }

    private void exigirContraproposta(String acao) {
        if (!isContraProposta()) {
            throw TransicaoInvalidaException.daProposta("Só uma contraproposta pode " + acao
                    + " — esta é uma proposta raiz (estado " + status + ")");
        }
    }

    private void exigirDonoDaRaiz(Usuario quem, String acao) {
        if (!propostaPai.getUsuario().mesmoUsuario(quem)) {
            throw new AcessoNegadoException("PROPOSTA_ACAO_NAO_AUTORIZADA",
                    "Só o dono da proposta original pode " + acao + " uma contraproposta");
        }
    }
}
