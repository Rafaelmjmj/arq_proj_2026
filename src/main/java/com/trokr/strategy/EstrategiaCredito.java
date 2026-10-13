package com.trokr.strategy;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;

/**
 * Strategy — a interface comum da família de algoritmos de crédito.
 *
 * Toda estratégia responde duas perguntas: "qual categoria você atende?"
 * e "quantos créditos este item vale?". Com categoria(), a
 * CalculadoraCreditoService monta o mapa de estratégias sem nenhum
 * if/switch por categoria.
 *
 * Uma categoria nova = uma classe nova com @Component. Nada que já existe
 * precisa mudar (Princípio Aberto/Fechado).
 */
public interface EstrategiaCredito {

    CategoriaItem categoria();

    int calcular(Item item);
}
