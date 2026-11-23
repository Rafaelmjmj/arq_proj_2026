package com.trokr.notificacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Um relógio que só anda quando o teste manda: "esperar 30 s" vira avancar(30 s). */
public class RelogioDeTeste extends Clock {

    private Instant agora = Instant.parse("2026-11-10T10:00:00Z");

    public void avancar(Duration tempo) {
        agora = agora.plus(tempo);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
