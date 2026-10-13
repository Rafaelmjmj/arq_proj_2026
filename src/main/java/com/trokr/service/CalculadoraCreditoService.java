package com.trokr.service;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import com.trokr.strategy.EstrategiaCredito;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Strategy — o CONTEXTO: escolhe a estratégia pela categoria do item.
 *
 * O Spring injeta TODAS as implementações de EstrategiaCredito na lista; o
 * construtor transforma a lista num mapa categoria → estratégia, uma única
 * vez. Nenhum if/switch por categoria aqui: o único if é uma guarda contra
 * categoria sem estratégia.
 *
 * Se duas estratégias declararem a mesma categoria, Collectors.toMap lança
 * IllegalStateException e a aplicação nem sobe — preferimos falhar cedo e
 * alto a escolher uma das duas em silêncio.
 */
@Service
public class CalculadoraCreditoService {

    private final Map<CategoriaItem, EstrategiaCredito> estrategias;

    public CalculadoraCreditoService(List<EstrategiaCredito> todas) {
        this.estrategias = todas.stream()
                .collect(Collectors.toMap(EstrategiaCredito::categoria, Function.identity()));
    }

    public int calcular(Item item) {
        EstrategiaCredito estrategia = estrategias.get(item.getCategoria());
        if (estrategia == null) {
            throw new IllegalStateException("Nenhuma estratégia de crédito para a categoria " + item.getCategoria());
        }
        return estrategia.calcular(item);
    }
}
