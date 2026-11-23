package com.trokr.factory;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.DadosDoTipo;
import com.trokr.model.Item;
import com.trokr.model.Usuario;

/**
 * Factory Method (Aula 12) — o contrato de toda fábrica de item.
 *
 * Quem pede um item entrega os dados e recebe um Item pronto e VÁLIDO, sem
 * saber qual subclasse veio. Cada tipo tem a sua fábrica, e é nela que mora
 * a regra "um produto só existe com peso": nenhum caminho do sistema
 * consegue criar um produto sem peso, nem por descuido.
 *
 * Mesma forma da Strategy da Aula 9 (interface + uma implementação por
 * categoria + mapa), intenção diferente: a Strategy calcula sobre um objeto
 * que já existe; a fábrica traz o objeto à existência.
 */
public interface FabricaDeItem {

    /** Qual categoria esta fábrica atende — a chave do catálogo. */
    CategoriaItem categoria();

    /**
     * Confere as regras de criação do tipo: exige o campo obrigatório e
     * rejeita os campos dos outros tipos (422). Usado na criação e na
     * atualização.
     */
    void validar(DadosDoTipo dados);

    /** Valida e monta o item com o builder. Não salva: isso é do service. */
    Item criar(ItemRequestDTO dados, Usuario proprietario);
}
