package com.trokr.strategy;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import org.springframework.stereotype.Component;

/**
 * Estratégia de crédito para EXPERIENCIA.
 *
 * Experiência (passeio guiado, jantar em casa): costuma ser única e envolver
 * deslocamento, por isso é a que mais vale. Na Aula 14 pode considerar o deslocamento.
 *
 * Testável sozinha com {@code new CreditoExperiencia()}, sem subir o Spring.
 */
@Component
public class CreditoExperiencia implements EstrategiaCredito {

    /** Valor decidido pela turma — ajuste aqui se a regra mudar. */
    public static final int CREDITOS = 20;

    @Override
    public CategoriaItem categoria() {
        return CategoriaItem.EXPERIENCIA;
    }

    @Override
    public int calcular(Item item) {
        return CREDITOS;
    }
}
