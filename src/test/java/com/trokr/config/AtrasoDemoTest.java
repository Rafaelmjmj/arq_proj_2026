package com.trokr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Aula 10 — o atraso de demonstração: desligado não espera, ligado espera. */
class AtrasoDemoTest {

    @Test
    void comZeroNaoEspera() {
        AtrasoDemo atraso = new AtrasoDemo(0);
        long inicio = System.nanoTime();
        atraso.aplicar();
        assertTrue((System.nanoTime() - inicio) / 1_000_000 < 50);
        assertEquals(0, atraso.getAtrasoMs());
    }

    @Test
    void ligadoEsperaPeloMenosOAtrasoConfigurado() {
        AtrasoDemo atraso = new AtrasoDemo(30);
        long inicio = System.nanoTime();
        atraso.aplicar();
        assertTrue((System.nanoTime() - inicio) / 1_000_000 >= 30);
    }
}
