package com.trokr.dto;

import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.NivelExperiencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Dados de entrada para criar/atualizar um Item. A requisição é uma só para
 * todos os tipos e diz qual tipo quer pela categoria (PRODUTO, SERVICO ou
 * EXPERIENCIA).
 *
 * Aula 12: os campos de cada tipo — pesoGramas (PRODUTO), duracaoMinutos
 * (SERVICO) e nivel (EXPERIENCIA: INICIANTE, INTERMEDIARIO, AVANCADO) — são
 * todos opcionais NA ESTRUTURA. O DTO não decide nada: quem sabe que um
 * produto exige peso e não pode ter nível é a fábrica daquele tipo.
 * Aqui só se barram valores sem sentido (peso ou duração ≤ 0 → 400).
 */
public record ItemRequestDTO(

        @Schema(description = "Título do item", example = "Bicicleta aro 29")
        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        @Schema(description = "Estado, detalhes, condições", example = "Pouco usada, revisada")
        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @Schema(description = "Tipo da oferta: decide qual fábrica valida e qual campo é obrigatório", example = "PRODUTO")
        @NotNull(message = "categoria é obrigatória (PRODUTO, SERVICO ou EXPERIENCIA)")
        CategoriaItem categoria,

        @Schema(description = "Dono do item", example = "1")
        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @Schema(description = "Só PRODUTO (obrigatório nele)", example = "14500")
        @Positive(message = "pesoGramas deve ser maior que zero")
        Integer pesoGramas,

        @Schema(description = "Só SERVICO (obrigatório nele)")
        @Positive(message = "duracaoMinutos deve ser maior que zero")
        Integer duracaoMinutos,

        @Schema(description = "Só EXPERIENCIA (obrigatório nela)")
        NivelExperiencia nivel
) {

    /** Os campos específicos de tipo, juntos, como a fábrica os confere. */
    public DadosDoTipo dadosDoTipo() {
        return new DadosDoTipo(pesoGramas, duracaoMinutos, nivel);
    }
}
