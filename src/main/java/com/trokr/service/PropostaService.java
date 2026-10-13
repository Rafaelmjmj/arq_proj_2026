package com.trokr.service;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.exception.ResourceNotFoundException;
import com.trokr.model.Item;
import com.trokr.model.Proposta;
import com.trokr.model.Usuario;
import com.trokr.repository.PropostaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * «Facade» — a porta de entrada única para tudo que envolve Proposta e
 * Contraproposta (Aula 9).
 *
 * <p>O PropostaController só conhece esta classe. Uma única chamada, como
 * {@link #finalizarNegociacao(Long)}, coordena por trás, SEMPRE nesta ordem:
 * <ol>
 *   <li>busca a Proposta ({@link PropostaRepository}, JPA/Postgres);</li>
 *   <li>aplica a transição — a Proposta delega para o estado atual
 *       (State, duas máquinas);</li>
 *   <li>o próprio domínio encerra as contrapropostas concorrentes
 *       (efeito cascata, também State);</li>
 *   <li>salva (JPA);</li>
 *   <li>se a troca chegou ao estado final, publica
 *       {@link TrocaConcluidaEvent} (Observer);</li>
 *   <li>os cinco listeners reagem sem que esta classe saiba que existem:
 *       Avaliação, Histórico, Notificação, LogEvento (Adapter → Mongo) e
 *       Créditos (Strategy).</li>
 * </ol>
 *
 * <p>O que ela deliberadamente NÃO faz:
 * <ul>
 *   <li>não decide se uma transição é válida — isso é das classes de estado;</li>
 *   <li>não valida quem pode aceitar/recusar — isso é da entidade Proposta;</li>
 *   <li>não calcula créditos, não cria avaliações, não grava log — isso é dos
 *       listeners e da Strategy.</li>
 * </ul>
 * Ela só sabe EM QUE ORDEM as coisas acontecem, não COMO cada uma é feita.
 * Por isso tem só 4 dependências e nenhum if de regra de negócio.
 */
@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final UsuarioService usuarioService;
    private final ItemService itemService;
    private final ApplicationEventPublisher eventPublisher;

    // =====================================================================
    // Consultas
    // =====================================================================

    public List<Proposta> listarTodas() {
        return propostaRepository.findAll();
    }

    public List<Proposta> listarRaizes() {
        return propostaRepository.findByPropostaPaiIsNull();
    }

    public List<Proposta> listarPorUsuario(Long usuarioId) {
        return propostaRepository.findByUsuarioId(usuarioId);
    }

    public Proposta buscarPorId(Long id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta não encontrada com id " + id));
    }

    /** As contrapropostas (concorrentes) de uma proposta raiz. */
    public List<Proposta> listarContrapropostas(Long propostaId) {
        buscarPorId(propostaId);
        return propostaRepository.findByPropostaPaiId(propostaId);
    }

    // =====================================================================
    // Criação
    // =====================================================================

    @Transactional
    public Proposta criarProposta(String titulo, String descricao, Long usuarioId, Long itemId) {
        Usuario autor = usuarioService.buscarPorId(usuarioId);
        Item item = itemService.buscarPorId(itemId);
        return propostaRepository.save(new Proposta(titulo, descricao, autor, item));
    }

    @Transactional
    public Proposta criarContraproposta(Long propostaId, String descricao, Long usuarioId, Long itemId) {
        Proposta raiz = buscarPorId(propostaId);
        Usuario autor = usuarioService.buscarPorId(usuarioId);
        Item item = itemService.buscarPorId(itemId);
        return propostaRepository.save(Proposta.criarContraProposta(descricao, autor, item, raiz));
    }

    // =====================================================================
    // Transições — cada uma: buscar, delegar ao domínio, salvar
    // =====================================================================

    @Transactional
    public Proposta submeter(Long id) {
        Proposta proposta = buscarPorId(id);
        proposta.submeter();
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta aprovar(Long id) {
        Proposta proposta = buscarPorId(id);
        proposta.aprovar();
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta voltarParaRascunho(Long id) {
        Proposta proposta = buscarPorId(id);
        proposta.voltarParaRascunho();
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta aceitar(Long contrapropostaId, Long usuarioId) {
        Proposta contraproposta = buscarPorId(contrapropostaId);
        contraproposta.aceitar(usuarioService.buscarPorId(usuarioId));
        return propostaRepository.save(contraproposta);
    }

    @Transactional
    public Proposta recusar(Long contrapropostaId, Long usuarioId) {
        Proposta contraproposta = buscarPorId(contrapropostaId);
        contraproposta.recusar(usuarioService.buscarPorId(usuarioId));
        return propostaRepository.save(contraproposta);
    }

    @Transactional
    public Proposta desistirDaNegociacao(Long contrapropostaId, Long usuarioId) {
        Proposta contraproposta = buscarPorId(contrapropostaId);
        contraproposta.desistirDaNegociacao(usuarioService.buscarPorId(usuarioId));
        return propostaRepository.save(contraproposta);
    }

    @Transactional
    public Proposta cancelar(Long id) {
        Proposta proposta = buscarPorId(id);
        proposta.cancelar();
        return propostaRepository.save(proposta);
    }

    /**
     * Finaliza a negociação. Aceita o id da raiz (Cenário A) ou da
     * contraproposta em negociação (Cenário B) — o domínio resolve os dois.
     * O evento só é publicado quando a troca chegou de fato ao estado final.
     */
    @Transactional
    public Proposta finalizarNegociacao(Long id) {
        Proposta proposta = buscarPorId(id);
        proposta.finalizar();
        propostaRepository.save(proposta);

        if (proposta.isTrocaConcluida()) {
            eventPublisher.publishEvent(construirEventoDeTrocaConcluida(proposta));
        }
        return proposta;
    }

    /**
     * Monta o payload nos dois cenários sem duplicar código: navega sempre
     * até a raiz, e da raiz até a contraproposta que fechou a troca.
     * A = dono da raiz e o item dele; B = autor da contraproposta e o item dele.
     */
    private TrocaConcluidaEvent construirEventoDeTrocaConcluida(Proposta proposta) {
        Proposta raiz = proposta.getRaiz();
        Proposta vencedora = raiz.getContrapropostaVencedora()
                .orElseThrow(() -> new IllegalStateException("Troca concluída sem contraproposta vencedora"));

        return new TrocaConcluidaEvent(
                raiz.getId(),
                raiz.getUsuario(),
                vencedora.getUsuario(),
                raiz.getItemOferecido(),
                vencedora.getItemOferecido(),
                LocalDateTime.now()
        );
    }
}
