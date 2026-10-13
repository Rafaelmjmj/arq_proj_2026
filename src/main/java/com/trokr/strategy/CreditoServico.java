package com.trokr.strategy;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import org.springframework.stereotype.Component;

/**
 * Estratégia de crédito para SERVICO.
 *
 * Serviço (aula de violão, revisão de texto): custa o tempo de quem oferece,
 * por isso vale mais que um produto. Na Aula 14 pode considerar a duração.
 *
 * Testável sozinha com {@code new CreditoServico()}, sem subir o Spring.
 */
@Component
public class CreditoServico implements EstrategiaCredito {

    /** Valor decidido pela turma — ajuste aqui se a regra mudar. */
    public static final int CREDITOS = 15;

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.SERVICO;
    }

    @Override
    public int calcular(Item item) {
        return CREDITOS;
    }
}
