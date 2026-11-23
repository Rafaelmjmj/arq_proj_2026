package com.trokr.exception;

import com.trokr.service.LogEventoService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * O contrato de erro do Trokr (Aula 11): TODA resposta de erro é um
 * ProblemDetail (RFC 9457, application/problem+json), com:
 * <ul>
 *   <li>{@code status} + {@code codigo} estável — para o código do cliente
 *       decidir o que fazer;</li>
 *   <li>{@code title} fixo e {@code detail} desta ocorrência — para a pessoa
 *       na tela;</li>
 *   <li>{@code instance} (o caminho) e, no 500, {@code ocorrencia} — para
 *       quem opera achar o erro no log.</li>
 * </ul>
 *
 * <p>A tabela de decisões (status · código · registro):
 * <table>
 *   <tr><td>RecursoNaoEncontradoException</td><td>404</td><td>específico</td><td>nenhum</td></tr>
 *   <tr><td>TransicaoInvalidaException</td><td>409</td><td>específico</td><td>nenhum</td></tr>
 *   <tr><td>RegraNegocioException</td><td>422</td><td>específico</td><td>nenhum</td></tr>
 *   <tr><td>AcessoNegadoException</td><td>403</td><td>específico</td><td>nenhum</td></tr>
 *   <tr><td>OptimisticLockingFailureException</td><td>409</td><td>PROPOSTA_ALTERADA_CONCORRENTEMENTE</td><td>log.warn</td></tr>
 *   <tr><td>DataIntegrityViolationException</td><td>409</td><td>RESTRICAO_DE_INTEGRIDADE</td><td>log.warn</td></tr>
 *   <tr><td>Validação (@Valid)</td><td>400</td><td>VALIDACAO + campos</td><td>nenhum</td></tr>
 *   <tr><td>Erros do Spring MVC (JSON, tipo, método)</td><td>400/405…</td><td>gerado pelo Spring</td><td>nenhum</td></tr>
 *   <tr><td>Qualquer outra Exception</td><td>500</td><td>ERRO_INTERNO</td><td>log.error + LogEvento</td></tr>
 * </table>
 * Só o que é DEFEITO vai para o LogEvento: erro do cliente não é erro do
 * sistema.
 *
 * <p>Herda de ResponseEntityExceptionHandler, que já transforma em
 * ProblemDetail as exceções do próprio Spring MVC (JSON quebrado, id não
 * numérico, método não suportado...). Aqui só se personaliza o resto.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TIPO_BASE = "https://trokr.local/erros/";

    private final LogEventoService logEventoService;

    // ------------------------------------------------------------------
    // Exceções de domínio: um handler para a hierarquia inteira
    // ------------------------------------------------------------------

    /**
     * TrokrException é sealed: o switch conhece todas as subclasses e deixa de
     * compilar se uma nova ficar de fora. Erro do cliente → nada no LogEvento.
     */
    @ExceptionHandler(TrokrException.class)
    public ProblemDetail handleDominio(TrokrException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex) {
            case RecursoNaoEncontradoException e -> HttpStatus.NOT_FOUND;
            case TransicaoInvalidaException e -> HttpStatus.CONFLICT;
            case RegraNegocioException e -> HttpStatus.UNPROCESSABLE_ENTITY;
            case AcessoNegadoException e -> HttpStatus.FORBIDDEN;
        };
        String titulo = switch (ex) {
            case RecursoNaoEncontradoException e -> "Recurso não encontrado";
            case TransicaoInvalidaException e -> "Ação não permitida no estado atual";
            case RegraNegocioException e -> "Regra de negócio violada";
            case AcessoNegadoException e -> "Ação não autorizada";
        };
        return problema(status, titulo, ex.getCodigo(), ex.getMessage(), request.getRequestURI());
    }

    // ------------------------------------------------------------------
    // Aula 10 — Corrida 1: quem perde a corrida recebe 409, não 500
    // ------------------------------------------------------------------

    /**
     * Duas requisições gravaram a mesma Proposta ao mesmo tempo; a segunda
     * validou com dado velho e o @Version recusou. Nada quebrou: houve um
     * conflito. Não tentamos de novo sozinhos — a repetição leria a Proposta
     * já Finalizada e o State recusaria do mesmo jeito.
     *
     * Um 409 de vez em quando é normal; muitos seguidos indicam um front-end
     * repetindo requisições. Por isso log.warn (no log da aplicação), e não
     * LogEvento de erro.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcorrencia(OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Alteração concorrente recusada em {}: {}", request.getRequestURI(), ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Alteração concorrente", "PROPOSTA_ALTERADA_CONCORRENTEMENTE",
                "Esta proposta foi alterada por outra requisição ao mesmo tempo. Recarregue a proposta e confira o estado atual.",
                request.getRequestURI());
    }

    /** Restrição do banco que escapou das regras do domínio (ex.: item em uso numa proposta). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Restrição de integridade violada em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito com dados existentes", "RESTRICAO_DE_INTEGRIDADE",
                "A operação viola uma restrição do banco: o registro está em uso ou já existe.",
                request.getRequestURI());
    }

    // ------------------------------------------------------------------
    // Validação (@Valid): 400 com a lista de campos
    // ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of("campo", erro.getField(),
                        "mensagem", String.valueOf(erro.getDefaultMessage())))
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "VALIDACAO",
                "Um ou mais campos estão inválidos. Confira a lista em 'campos'.", caminho(request));
        pd.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(pd);
    }

    // ------------------------------------------------------------------
    // O catch-all: o único que não pode contar o que aconteceu
    // ------------------------------------------------------------------

    /**
     * Defeito do servidor. Para o cliente: 500, ERRO_INTERNO, uma frase
     * genérica e o id da ocorrência — nunca ex.getMessage() (pode ter SQL,
     * tabela, caminho de arquivo). Para quem opera: log.error com o stack
     * trace e o LogEvento ERRO_SISTEMA, os dois com o MESMO id.
     *
     * IllegalStateException e IllegalArgumentException que sobrarem caem
     * aqui de propósito: depois da Aula 11, o domínio só recusa com
     * TrokrException, então elas indicam defeito.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex, HttpServletRequest request) {
        String ocorrencia = UUID.randomUUID().toString();
        log.error("Erro inesperado [ocorrencia={}] em {}", ocorrencia, request.getRequestURI(), ex);
        logEventoService.registrarErro("GlobalExceptionHandler", "Erro inesperado em " + request.getRequestURI(),
                ex, ocorrencia);

        ProblemDetail pd = problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Se persistir, informe ao suporte o código da ocorrência.",
                request.getRequestURI());
        pd.setProperty("ocorrencia", ocorrencia);
        return pd;
    }

    // ------------------------------------------------------------------
    // Um único lugar monta todos os ProblemDetail
    // ------------------------------------------------------------------

    private ProblemDetail problema(HttpStatus status, String titulo, String codigo, String detalhe, String caminho) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(titulo);
        pd.setType(URI.create(TIPO_BASE + codigo.toLowerCase().replace('_', '-')));
        if (caminho != null) {
            pd.setInstance(URI.create(caminho));
        }
        pd.setProperty("codigo", codigo);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    private String caminho(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
