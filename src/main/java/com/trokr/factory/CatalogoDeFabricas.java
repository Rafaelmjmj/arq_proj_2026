package com.trokr.factory;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.exception.RegraNegocioException;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import com.trokr.model.Usuario;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * O catálogo que escolhe a fábrica pela categoria (Aula 12).
 *
 * O Spring injeta TODAS as FabricaDeItem numa lista; o construtor monta o
 * mapa categoria → fábrica uma única vez (a mesma técnica de registro da
 * CalculadoraCreditoService). Nenhum switch, nenhum new de subclasse: o
 * ItemService só chama catalogo.criar(...) e salva.
 *
 * Um tipo novo (ex.: ITEM DIGITAL) = ItemDigital + FabricaItemDigital +
 * CreditoDigital, e só o enum e o DTO mudam. Nem este catálogo, nem o
 * ItemService, nem a calculadora, nem a Proposta.
 */
@Component
public class CatalogoDeFabricas {

    private final Map<CategoriaItem, FabricaDeItem> fabricas;

    public CatalogoDeFabricas(List<FabricaDeItem> todas) {
        this.fabricas = todas.stream()
                .collect(Collectors.toMap(FabricaDeItem::categoria, Function.identity()));
    }

    /** Escolhe a fábrica pela categoria pedida, que valida e monta o item. */
    public Item criar(ItemRequestDTO dados, Usuario proprietario) {
        return fabrica(dados.categoria()).criar(dados, proprietario);
    }

    /**
     * Na atualização, um produto não vira serviço: mudar o tipo é criar outro
     * item (422 ITEM_CATEGORIA_IMUTAVEL). Mantida a categoria, a fábrica do
     * tipo confere os campos como na criação.
     */
    public void validarAtualizacao(Item existente, ItemRequestDTO dados) {
        if (dados.categoria() != existente.getCategoria()) {
            throw new RegraNegocioException("ITEM_CATEGORIA_IMUTAVEL",
                    "Um item " + existente.getCategoria() + " não pode virar " + dados.categoria()
                            + " — para mudar o tipo, cadastre outro item");
        }
        fabrica(existente.getCategoria()).validar(dados.dadosDoTipo());
    }

    private FabricaDeItem fabrica(CategoriaItem categoria) {
        FabricaDeItem fabrica = fabricas.get(categoria);
        if (fabrica == null) {
            // Defeito de configuração (categoria sem fábrica), não regra de negócio → 500.
            throw new IllegalStateException("Nenhuma fábrica de item para a categoria " + categoria);
        }
        return fabrica;
    }
}
