package com.trokr.dto;

import com.trokr.model.Proposta;
import com.trokr.model.Status;
import java.time.LocalDateTime;

/**
 * Dados de saída de uma Proposta ou Contraproposta. "tipo" diz qual das
 * duas é (RAIZ ou CONTRAPROPOSTA); "podeMudar" é false em estado terminal.
 */
public record PropostaResponseDTO(
        Long id,
        String tipo,
        String titulo,
        String descricao,
        Status status,
        boolean podeMudar,
        Long usuarioId,
        String usuarioNome,
        Long itemId,
        String itemTitulo,
        String itemCategoria,
        Long propostaPaiId,
        LocalDateTime dataCriacao
) {

    public static PropostaResponseDTO fromEntity(Proposta proposta) {
        return new PropostaResponseDTO(
                proposta.getId(),
                proposta.isContraProposta() ? "CONTRAPROPOSTA" : "RAIZ",
                proposta.getTitulo(),
                proposta.getDescricao(),
                proposta.getStatus(),
                proposta.isPodeMudar(),
                proposta.getUsuario().getId(),
                proposta.getUsuario().getNome(),
                proposta.getItemOferecido().getId(),
                proposta.getItemOferecido().getTitulo(),
                proposta.getItemOferecido().getCategoria().name(),
                proposta.isContraProposta() ? proposta.getPropostaPai().getId() : null,
                proposta.getDataCriacao()
        );
    }
}
