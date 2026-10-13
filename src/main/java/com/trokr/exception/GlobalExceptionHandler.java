package com.trokr.exception;

import com.trokr.service.LogEventoService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz exceções em respostas HTTP. O tratamento completo e padronizado de
 * erros é conteúdo da Aula 11; por enquanto:
 * <ul>
 *   <li>404 — recurso não encontrado;</li>
 *   <li>400 — dados inválidos (@Valid) ou regra de entrada violada;</li>
 *   <li>403 — alguém agindo em nome de outra pessoa;</li>
 *   <li>409 — transição de estado inválida (o State recusou);</li>
 *   <li>500 — qualquer outra coisa, registrada como ERRO_SISTEMA no LogEvento.</li>
 * </ul>
 * Transições inválidas e erros inesperados também vão para o LogEvento
 * (Mongo) — é o "ponto de erro" sugerido na Aula 8.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LogEventoService logEventoService;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(campo -> erros.put(campo.getField(), campo.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erros);
    }

    /** JSON malformado ou valor inválido, ex.: "categoria": "CARRO". */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return erro(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido — confira o JSON e os valores dos enums");
    }

    /** Parâmetro de URL com tipo errado, ex.: ?usuarioId=abc ou ?categoria=CARRO. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return erro(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleArgumentoInvalido(IllegalArgumentException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, String>> handleAcessoNegado(AcessoNegadoException ex) {
        logEventoService.registrarAviso("ACESSO_NEGADO", "GlobalExceptionHandler", ex.getMessage(), Map.of());
        return erro(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleTransicaoInvalida(IllegalStateException ex) {
        logEventoService.registrarAviso("TRANSICAO_INVALIDA", "GlobalExceptionHandler", ex.getMessage(), Map.of());
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Violação de restrição do banco, ex.: e-mail duplicado ou item em uso numa proposta. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegridade(DataIntegrityViolationException ex) {
        return erro(HttpStatus.CONFLICT, "Operação viola uma restrição do banco (registro duplicado ou em uso)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleInesperado(Exception ex) {
        // Exceções do próprio Spring MVC (JSON malformado, rota inexistente,
        // parâmetro faltando...) já sabem o status HTTP correto.
        if (ex instanceof ErrorResponse erroDoSpring) {
            return ResponseEntity.status(erroDoSpring.getStatusCode())
                    .body(Map.of("erro", String.valueOf(ex.getMessage())));
        }
        logEventoService.registrarErro("GlobalExceptionHandler", "Erro não tratado", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", String.valueOf(mensagem)));
    }
}
