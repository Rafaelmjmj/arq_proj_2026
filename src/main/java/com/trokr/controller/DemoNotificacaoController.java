package com.trokr.controller;

import com.trokr.notificacao.SimulacaoNotificacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interruptor do serviço de notificação simulado (Aula 13). Só existe com
 * {@code trokr.demo.endpoints: true}: com a chave desligada o bean não é
 * criado, a rota responde 404 e o grupo some do Swagger (Aula 14).
 */
@Tag(name = "Demonstração",
        description = "Liga e desliga o serviço de notificação simulado para ver o retry e o disjuntor. "
                + "Só existe com trokr.demo.endpoints=true.")
@RestController
@RequestMapping("/demo/notificacao")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trokr.demo.endpoints", havingValue = "true")
public class DemoNotificacaoController {

    private final SimulacaoNotificacao simulacao;

    @Operation(summary = "Derruba o serviço de notificação",
            description = "Toda chamada passa a falhar. Finalize trocas e veja em GET /logs as tentativas, "
                    + "o disjuntor abrindo e as NOTIFICACAO_PENDENTE.")
    @ApiResponse(responseCode = "204", description = "Serviço fora do ar")
    @PatchMapping("/fora-do-ar")
    public ResponseEntity<Void> derrubar() {
        simulacao.derrubar();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Religa o serviço de notificação",
            description = "Nenhuma chamada falha. Se o disjuntor estiver aberto, ele fecha na primeira "
                    + "notificação depois do tempo-aberto (30 s).")
    @ApiResponse(responseCode = "204", description = "Serviço no ar")
    @PatchMapping("/no-ar")
    public ResponseEntity<Void> religar() {
        simulacao.religar();
        return ResponseEntity.noContent().build();
    }
}
