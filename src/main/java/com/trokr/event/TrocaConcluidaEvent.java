package com.trokr.event;

import com.trokr.model.Item;
import com.trokr.model.Usuario;
import java.time.LocalDateTime;

/**
 * Observer — o EVENTO: "a troca terminou, entre essas pessoas" (Aula 7).
 *
 * Só descreve o fato; não tem nenhum método de ação. Quem reage é decisão de
 * quem escuta (os listeners), não do evento. Também não carrega status nem
 * qual máquina finalizou — isso já é passado.
 *
 * Convenção dos lados: A é o dono da Proposta raiz; B é o autor da
 * contraproposta que fechou a troca. Cada um ofereceu o seu item.
 *
 * @param propostaId    id da Proposta RAIZ — sempre ela, mesmo quando quem
 *                      finalizou foi a contraproposta
 * @param usuarioA      dono da proposta raiz
 * @param usuarioB      autor da contraproposta vencedora
 * @param itemA         item oferecido por A (na proposta raiz)
 * @param itemB         item oferecido por B (na contraproposta)
 * @param dataConclusao quando a troca foi finalizada
 */
public record TrocaConcluidaEvent(
        Long propostaId,
        Usuario usuarioA,
        Usuario usuarioB,
        Item itemA,
        Item itemB,
        LocalDateTime dataConclusao
) {
}
