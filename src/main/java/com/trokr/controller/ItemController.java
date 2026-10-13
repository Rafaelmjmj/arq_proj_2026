package com.trokr.controller;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.dto.ItemResponseDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import com.trokr.service.ItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponseDTO> listar() {
        return paraDTO(itemService.listarTodos());
    }

    @GetMapping("/{id}")
    public ItemResponseDTO buscarPorId(@PathVariable Long id) {
        return ItemResponseDTO.fromEntity(itemService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public List<ItemResponseDTO> listarPorTitulo(@RequestParam("titulo") String titulo) {
        return paraDTO(itemService.listarPorTitulo(titulo));
    }

    @GetMapping("/buscar-categoria")
    public List<ItemResponseDTO> listarPorCategoria(@RequestParam("categoria") CategoriaItem categoria) {
        return paraDTO(itemService.listarPorCategoria(categoria));
    }

    @GetMapping("/buscar-itens-usuario/{id}")
    public List<ItemResponseDTO> listarPorProprietario(@PathVariable Long id) {
        return paraDTO(itemService.listarPorUsuarioProprietario(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponseDTO> criar(@Valid @RequestBody ItemRequestDTO dto) {
        Item item = new Item(dto.titulo(), dto.descricao(), dto.categoria(), null);
        Item salvo = itemService.criar(item, dto.usuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponseDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public ItemResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ItemRequestDTO dto) {
        Item dadosAtualizados = new Item(dto.titulo(), dto.descricao(), dto.categoria(), null);
        return ItemResponseDTO.fromEntity(itemService.atualizar(id, dadosAtualizados, dto.usuarioId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        itemService.remover(id);
        return ResponseEntity.noContent().build();
    }

    private List<ItemResponseDTO> paraDTO(List<Item> itens) {
        return itens.stream().map(ItemResponseDTO::fromEntity).toList();
    }
}
