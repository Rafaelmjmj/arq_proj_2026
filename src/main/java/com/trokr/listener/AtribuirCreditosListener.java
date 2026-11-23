package com.trokr.listener;

import com.trokr.event.TrocaConcluidaEvent;
import com.trokr.model.Item;
import com.trokr.model.Usuario;
import com.trokr.repository.UsuarioRepository;
import com.trokr.service.CalculadoraCreditoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener 5 de 5 (Aula 9) — cada usuário ganha créditos pelo item que
 * OFERECEU. Não sabe calcular crédito: pede à CalculadoraCreditoService,
 * que escolhe a estratégia pela categoria do item (Strategy).
 *
 * Aula 10, Corrida 2: antes o listener buscava o Usuario, somava no objeto
 * Java e salvava (read-modify-write) — duas trocas do mesmo usuário ao mesmo
 * tempo perdiam créditos. Agora a Strategy continua decidindo QUANTO
 * creditar; só mudou o jeito de GRAVAR: um UPDATE atômico no banco.
 *
 * Fica DENTRO da transação da finalização (@EventListener): se ela sofrer
 * rollback (Corrida 1), o crédito também é desfeito.
 *
 * Os dois créditos são aplicados SEMPRE em ordem crescente de id do usuário.
 * Cada UPDATE trava a linha do usuário até o commit; se a troca A↔B e a troca
 * B↔A fossem finalizadas ao mesmo tempo creditando em ordens opostas, uma
 * esperaria a outra para sempre (deadlock, Aula 10). Com a mesma ordem para
 * todos, o ciclo não se forma.
 */
@Component
@RequiredArgsConstructor
public class AtribuirCreditosListener {

    private final CalculadoraCreditoService calculadora;
    private final UsuarioRepository usuarioRepository;

    @EventListener
    public void aoConcluirTroca(TrocaConcluidaEvent evento) {
        if (evento.usuarioA().getId() <= evento.usuarioB().getId()) {
            creditar(evento.usuarioA(), evento.itemA());
            creditar(evento.usuarioB(), evento.itemB());
        } else {
            creditar(evento.usuarioB(), evento.itemB());
            creditar(evento.usuarioA(), evento.itemA());
        }
    }

    private void creditar(Usuario usuario, Item itemOferecido) {
        int creditos = calculadora.calcular(itemOferecido);
        int linhas = usuarioRepository.adicionarCreditos(usuario.getId(), creditos);
        if (linhas != 1) {
            // Defeito: o usuário veio de uma Proposta válida e tinha de existir.
            // Exceção técnica → 500 e rollback da troca inteira.
            throw new IllegalStateException("Crédito não aplicado ao usuário " + usuario.getId()
                    + " (linhas afetadas: " + linhas + ")");
        }
    }
}
