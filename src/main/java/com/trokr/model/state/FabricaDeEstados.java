package com.trokr.model.state;

import com.trokr.model.Status;
import com.trokr.model.state.contraproposta.ContraAnalise;
import com.trokr.model.state.contraproposta.ContraCancelado;
import com.trokr.model.state.contraproposta.ContraFinalizado;
import com.trokr.model.state.contraproposta.ContraNegociado;
import com.trokr.model.state.contraproposta.ContraRascunho;
import com.trokr.model.state.contraproposta.ContraRecusado;
import com.trokr.model.state.contraproposta.EstadoContraproposta;
import com.trokr.model.state.proposta.EstadoAtiva;
import com.trokr.model.state.proposta.EstadoCancelado;
import com.trokr.model.state.proposta.EstadoFinalizado;
import com.trokr.model.state.proposta.EstadoHomologacao;
import com.trokr.model.state.proposta.EstadoNegociado;
import com.trokr.model.state.proposta.EstadoProposta;
import com.trokr.model.state.proposta.EstadoRascunho;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reconstrói o objeto de estado a partir do enum salvo no banco.
 *
 * Por que isso existe: o JPA só sabe persistir o campo {@code status}
 * (enum). O objeto de estado (EstadoAtiva, ContraAnalise...) não vai para o
 * banco. Quando uma Proposta é carregada, ela usa esta fábrica para
 * "reidratar" o estado atual. Um mapa por máquina (Opção A: duas máquinas
 * independentes) — RASCUNHO aparece nos dois, cada um com sua classe.
 */
public final class FabricaDeEstados {

    private static final Map<Status, Supplier<EstadoProposta>> RAIZ = Map.of(
            Status.RASCUNHO, EstadoRascunho::new,
            Status.HOMOLOGACAO, EstadoHomologacao::new,
            Status.ATIVA, EstadoAtiva::new,
            Status.NEGOCIADO_RAIZ, EstadoNegociado::new,
            Status.FINALIZADO, EstadoFinalizado::new,
            Status.CANCELADO_RAIZ, EstadoCancelado::new
    );

    private static final Map<Status, Supplier<EstadoContraproposta>> CONTRA = Map.of(
            Status.RASCUNHO, ContraRascunho::new,
            Status.EM_ANALISE, ContraAnalise::new,
            Status.NEGOCIADO_CONTRA, ContraNegociado::new,
            Status.FINALIZADO_CONTRA, ContraFinalizado::new,
            Status.RECUSADO, ContraRecusado::new,
            Status.CANCELADO_CONTRA, ContraCancelado::new
    );

    private FabricaDeEstados() {
    }

    public static EstadoProposta estadoRaiz(Status status) {
        return buscar(RAIZ, status, "proposta raiz");
    }

    public static EstadoContraproposta estadoContraproposta(Status status) {
        return buscar(CONTRA, status, "contraproposta");
    }

    private static <T> T buscar(Map<Status, Supplier<T>> mapa, Status status, String maquina) {
        Supplier<T> fabrica = mapa.get(status);
        if (fabrica == null) {
            throw new IllegalStateException("Status " + status + " não existe na máquina de " + maquina);
        }
        return fabrica.get();
    }
}
