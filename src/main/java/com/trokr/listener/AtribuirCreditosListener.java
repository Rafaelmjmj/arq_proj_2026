package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.Usuario;
import com.trokr.repository.UsuarioRepository;
import com.trokr.service.CalculadoraCreditoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 5 de 5 (Aula 9) — cada usuário ganha créditos pelo item que
 * OFERECEU. Não sabe calcular crédito: pede à CalculadoraCreditoService,
 * que escolhe a estratégia pela categoria do item (Strategy).
 *
 * O Observer pagando dividendos: para os créditos existirem, nenhuma linha
 * do PropostaService, do evento ou dos outros quatro listeners mudou.
 */
@Component
@RequiredArgsConstructor
public class AtribuirCreditosListener {

    private final CalculadoraCreditoService calculadora;
    private final UsuarioRepository usuarioRepository;

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        Usuario usuarioA = evento.usuarioA();
        Usuario usuarioB = evento.usuarioB();

        usuarioA.creditar(calculadora.calcular(evento.itemA()));
        usuarioB.creditar(calculadora.calcular(evento.itemB()));

        usuarioRepository.saveAll(List.of(usuarioA, usuarioB));
    }
}
