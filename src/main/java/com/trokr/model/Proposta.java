package com.trokr.model;

import com.trokr.exception.AcessoNegadoException;
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
            throw new IllegalArgumentException("O item " + itemOferecido.getId()
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
            throw new IllegalArgumentException("Não é possível contrapropor uma contraproposta — responda à proposta original");
        }
        if (pai.getUsuario().mesmoUsuario(autor)) {
            throw new IllegalArgumentException("O autor não pode contrapropor a própria proposta");
        }
        if (!pai.estadoRaiz().podeReceberContraproposta()) {
            throw new IllegalStateException("Só propostas Ativas recebem contrapropostas (status atual: "
                    + pai.getStatus() + ")");
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

    /**
     * Só contraproposta: o dono da Proposta original aceita esta
     * contraproposta. Contra: Em Análise → Negociado; raiz: Ativa → Negociado.
     */
    public void aceitar(Usuario quemAceita) {
        exigirContraproposta("aceitar");
        exigirDonoDaRaiz(quemAceita, "aceitar");
        estadoContra().irParaNegociado(this);
        propostaPai.estadoRaiz().irParaNegociado(propostaPai);
    }

    /** Só contraproposta: o dono da Proposta original recusa. Em Análise → Recusado. */
    public void recusar(Usuario quemRecusa) {
        exigirContraproposta("recusar");
        exigirDonoDaRaiz(quemRecusa, "recusar");
        estadoContra().irParaRecusado(this);
    }

    /**
     * Só contraproposta em negociação: uma das duas partes desiste.
     * Contra: Negociado → Recusado; raiz: Negociado → Ativa (volta a receber
     * contrapropostas).
     */
    public void desistirDaNegociacao(Usuario quemDesiste) {
        exigirContraproposta("desistir da negociação");
        if (!usuario.mesmoUsuario(quemDesiste) && !propostaPai.getUsuario().mesmoUsuario(quemDesiste)) {
            throw new AcessoNegadoException("Só as duas partes da negociação podem desistir dela");
        }
        estadoContra().irParaRecusado(this);
        propostaPai.estadoRaiz().irParaAtiva(propostaPai);
    }

    /**
     * Cancela. Numa raiz, dispara o efeito cascata: as contrapropostas ainda
     * abertas são encerradas (cada estado decide como).
     */
    public void cancelar() {
        if (isContraProposta()) {
            estadoContra().irParaCancelado(this);
        } else {
            estadoRaiz().irParaCancelado(this);
            encerrarContrapropostas();
        }
    }

    /**
     * Finaliza a troca — pode ser chamado a partir da raiz (Cenário A) ou da
     * contraproposta em negociação (Cenário B). Nos dois casos:
     * contraproposta → Finalizado, raiz → Finalizado e as concorrentes que
     * ainda estavam abertas são encerradas (efeito cascata).
     */
    public void finalizar() {
        Proposta raiz = getRaiz();
        Proposta contra = isContraProposta()
                ? this
                : raiz.getContrapropostaEmNegociacao().orElseThrow(() ->
                        new IllegalStateException("Nenhuma contraproposta em negociação para finalizar"));

        contra.estadoContra().irParaFinalizado(contra);
        raiz.estadoRaiz().irParaFinalizado(raiz);
        raiz.encerrarContrapropostas();
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
        return getRaiz().getStatus() == Status.FINALIZADO;
    }

    public List<Proposta> getContrapropostas() {
        return Collections.unmodifiableList(contrapropostas);
    }

    /** A contraproposta que está em negociação agora (no máximo uma). */
    public Optional<Proposta> getContrapropostaEmNegociacao() {
        return buscarContrapropostaPorStatus(Status.NEGOCIADO_CONTRA);
    }

    /** A contraproposta que fechou a troca, depois de finalizada. */
    public Optional<Proposta> getContrapropostaVencedora() {
        return buscarContrapropostaPorStatus(Status.FINALIZADO_CONTRA);
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

    /** Efeito cascata: pede para cada contraproposta se encerrar. */
    protected void encerrarContrapropostas() {
        contrapropostas.forEach(c -> c.estadoContra().encerrarPorFimDaRaiz(c));
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

    private void exigirRaiz(String acao) {
        if (isContraProposta()) {
            throw new IllegalStateException("Só uma proposta raiz pode " + acao);
        }
    }

    private void exigirContraproposta(String acao) {
        if (!isContraProposta()) {
            throw new IllegalStateException("Só uma contraproposta pode " + acao);
        }
    }

    private void exigirDonoDaRaiz(Usuario quem, String acao) {
        if (!propostaPai.getUsuario().mesmoUsuario(quem)) {
            throw new AcessoNegadoException("Só o dono da proposta original pode " + acao + " uma contraproposta");
        }
    }
}
