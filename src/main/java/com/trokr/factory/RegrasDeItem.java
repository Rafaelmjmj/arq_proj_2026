package com.trokr.factory;

import com.trokr.exception.RegraNegocioException;
import com.trokr.model.CategoriaItem;

/**
 * As duas checagens que toda fábrica repete, escritas uma vez só. Lançam as
 * exceções da hierarquia da Aula 11; o handler já sabe transformar cada uma
 * em resposta 422 com o código certo.
 */
final class RegrasDeItem {

    private RegrasDeItem() {
    }

    /** O campo do tipo é obrigatório → 422 ITEM_DADOS_INCOMPLETOS. */
    static void exigir(Object valor, String campo, CategoriaItem categoria) {
        if (valor == null) {
            throw new RegraNegocioException("ITEM_DADOS_INCOMPLETOS",
                    "Um item da categoria " + categoria + " precisa informar " + campo);
        }
    }

    /** Campo de outro tipo é rejeitado, não ignorado em silêncio → 422 ITEM_CAMPO_INCOMPATIVEL. */
    static void rejeitar(Object valor, String campo, CategoriaItem categoria) {
        if (valor != null) {
            throw new RegraNegocioException("ITEM_CAMPO_INCOMPATIVEL",
                    "Um item da categoria " + categoria + " não tem o campo " + campo);
        }
    }
}
