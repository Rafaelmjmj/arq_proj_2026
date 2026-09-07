package com.trokr.controller;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.dto.ItemResponseDTO;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponseDTO> listar() {
        return itemService.listarTodos().stream()
                .map(ItemResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ItemResponseDTO buscarPorId(@PathVariable Long id) {
        return ItemResponseDTO.fromEntity(itemService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponseDTO> criar(@Valid @RequestBody ItemRequestDTO dto) {
        Item item = new Item();
        item.setTitulo(dto.titulo());
        item.setDescricao(dto.descricao());

        Item salvo = itemService.criar(item, dto.usuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponseDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public ItemResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ItemRequestDTO dto) {
        Item dadosAtualizados = new Item();
        dadosAtualizados.setTitulo(dto.titulo());
        dadosAtualizados.setDescricao(dto.descricao());

        return ItemResponseDTO.fromEntity(itemService.atualizar(id, dadosAtualizados, dto.usuarioId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        itemService.remover(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/titulo")
    public ItemResponseDTO buscarPorTitulo(@RequestParam String titulo) {
        return ItemResponseDTO.fromEntity(
            itemService.buscarPorTitulo(titulo)
     );
    }
    @GetMapping("/descricao")
    public ItemResponseDTO buscarPorDescricao(@RequestParam String descricao) {
        return ItemResponseDTO.fromEntity(
            itemService.buscarPorDescricao(descricao)
        );
    }
    @GetMapping("/buscar/titulo")
    public List<ItemResponseDTO> buscarPorTituloContaining(
        @RequestParam String titulo) {

    return itemService.buscarPorTituloContaining(titulo).stream()
            .map(ItemResponseDTO::fromEntity)
            .toList();
    }
    @GetMapping("/buscar/descricao")
    public List<ItemResponseDTO> buscarPorDescricaoContaining(
        @RequestParam String descricao) {

    return itemService.buscarPorDescricaoContaining(descricao).stream()
            .map(ItemResponseDTO::fromEntity)
            .toList();
    }
    @GetMapping("/usuario/{usuarioId}")
    public List<ItemResponseDTO> listarPorUsuario(
        @PathVariable Long usuarioId) {

    return itemService.listarPorUsuario(usuarioId).stream()
            .map(ItemResponseDTO::fromEntity)
            .toList();
    }   
}
