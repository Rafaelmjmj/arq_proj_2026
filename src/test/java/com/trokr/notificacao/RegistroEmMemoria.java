package com.trokr.notificacao;

import com.trokr.adapter.EventoRegistrado;
import com.trokr.adapter.NivelLog;
import com.trokr.adapter.RegistroDeEventos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** O "Mongo" em memória: um adaptador de teste da porta RegistroDeEventos (Aula 8). */
public class RegistroEmMemoria implements RegistroDeEventos {

    public final List<EventoRegistrado> eventos = new ArrayList<>();

    @Override
    public EventoRegistrado registrar(EventoRegistrado evento) {
        eventos.add(evento);
        return evento;
    }

    @Override
    public List<EventoRegistrado> buscarPorTipo(String tipo) {
        return eventos.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    @Override
    public List<EventoRegistrado> buscarPorNivelDesde(NivelLog nivel, LocalDateTime desde) {
        return eventos.stream().filter(e -> e.nivel() == nivel).toList();
    }
}
