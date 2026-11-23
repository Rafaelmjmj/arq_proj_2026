package com.trokr.service;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.exception.RecursoNaoEncontradoException;
import com.trokr.factory.CatalogoDeFabricas;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import com.trokr.model.Usuario;
import com.trokr.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aula 12 — o service não sabe montar item nenhum: pede ao catálogo de
 * fábricas, que escolhe a fábrica pela categoria, valida e devolve o item
 * pronto pelo builder. Aqui não há switch nem new de subclasse; um tipo
 * novo de item não muda uma linha desta classe.
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UsuarioService usuarioService;
    private final CatalogoDeFabricas catalogoDeFabricas;

    public List<Item> listarTodos() {
        return itemRepository.findAll();
    }

    public Item buscarPorId(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.item(id));
    }

    public List<Item> listarPorTitulo(String titulo) {
        return itemRepository.findByTituloContainingIgnoreCase(titulo);
    }

    public List<Item> listarPorCategoria(CategoriaItem categoria) {
        return itemRepository.findByCategoria(categoria.name());
    }

    public List<Item> listarPorUsuarioProprietario(Long usuarioId) {
        return itemRepository.findByUsuarioProprietarioId(usuarioId);
    }

    public Item criar(ItemRequestDTO dados) {
        Usuario dono = usuarioService.buscarPorId(dados.usuarioId());
        Item item = catalogoDeFabricas.criar(dados, dono);
        return itemRepository.save(item);
    }

    /**
     * O tipo de um item não muda (ITEM_CATEGORIA_IMUTAVEL); dentro do tipo, a
     * fábrica confere os campos e o próprio item se atualiza.
     */
    public Item atualizar(Long id, ItemRequestDTO dados) {
        Item existente = buscarPorId(id);
        Usuario dono = usuarioService.buscarPorId(dados.usuarioId());
        catalogoDeFabricas.validarAtualizacao(existente, dados);
        existente.atualizar(dados.titulo(), dados.descricao(), dono, dados.dadosDoTipo());
        return itemRepository.save(existente);
    }

    public void remover(Long id) {
        Item item = buscarPorId(id);
        itemRepository.delete(item);
    }
}
