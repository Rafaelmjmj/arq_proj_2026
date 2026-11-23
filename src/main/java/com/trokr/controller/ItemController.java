package com.trokr.controller;

import com.trokr.dto.ItemRequestDTO;
import com.trokr.dto.ItemResponseDTO;
import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import com.trokr.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Itens", description = "O que cada usuário oferece. Cada categoria tem o seu campo obrigatório: "
        + "pesoGramas (PRODUTO), duracaoMinutos (SERVICO) ou nivel (EXPERIENCIA). "
        + "Quem valida é a fábrica do tipo (Factory Method).")
@RestController
@RequestMapping("/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "Lista todos os itens",
            description = "Cada item traz só o campo do seu tipo.")
    @GetMapping
    public List<ItemResponseDTO> listar() {
        return paraDTO(itemService.listarTodos());
    }

    @Operation(summary = "Busca um item pelo id")
    @ApiResponse(responseCode = "200", description = "Item encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ItemResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "ITEM_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ItemResponseDTO buscarPorId(
            @Parameter(description = "Id do item", example = "1") @PathVariable Long id) {
        return ItemResponseDTO.fromEntity(itemService.buscarPorId(id));
    }

    @Operation(summary = "Lista itens cujo título contém o texto")
    @GetMapping("/buscar")
    public List<ItemResponseDTO> listarPorTitulo(
            @Parameter(description = "Parte do título", example = "bici") @RequestParam("titulo") String titulo) {
        return paraDTO(itemService.listarPorTitulo(titulo));
    }

    @Operation(summary = "Lista os itens de uma categoria")
    @GetMapping("/buscar-categoria")
    public List<ItemResponseDTO> listarPorCategoria(@RequestParam("categoria") CategoriaItem categoria) {
        return paraDTO(itemService.listarPorCategoria(categoria));
    }

    @Operation(summary = "Lista os itens de um usuário")
    @GetMapping("/buscar-itens-usuario/{id}")
    public List<ItemResponseDTO> listarPorProprietario(
            @Parameter(description = "Id do dono", example = "1") @PathVariable Long id) {
        return paraDTO(itemService.listarPorUsuarioProprietario(id));
    }

    @Operation(summary = "Cadastra um item",
            description = "A fábrica da categoria exige o campo do tipo e recusa os campos dos outros tipos.")
    @ApiResponse(responseCode = "201", description = "Item criado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido, ex.: pesoGramas ≤ 0 (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Dono inexistente (USUARIO_NAO_ENCONTRADO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Falta o campo do tipo (ITEM_DADOS_INCOMPLETOS) ou veio campo de outro tipo (ITEM_CAMPO_INCOMPATIVEL)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<ItemResponseDTO> criar(@Valid @RequestBody ItemRequestDTO dto) {
        Item salvo = itemService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponseDTO.fromEntity(salvo));
    }

    @Operation(summary = "Atualiza um item",
            description = "A categoria não muda: para trocar o tipo, cadastre outro item.")
    @ApiResponse(responseCode = "200", description = "Item atualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista 'campos' diz o que corrigir (VALIDACAO)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "ITEM_NAO_ENCONTRADO ou USUARIO_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Categoria diferente da atual (ITEM_CATEGORIA_IMUTAVEL) ou campos inválidos para o tipo",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public ItemResponseDTO atualizar(
            @Parameter(description = "Id do item", example = "1") @PathVariable Long id,
            @Valid @RequestBody ItemRequestDTO dto) {
        return ItemResponseDTO.fromEntity(itemService.atualizar(id, dto));
    }

    @Operation(summary = "Remove um item")
    @ApiResponse(responseCode = "204", description = "Item removido")
    @ApiResponse(responseCode = "404", description = "ITEM_NAO_ENCONTRADO",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O item está em uso numa proposta (RESTRICAO_DE_INTEGRIDADE)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @Parameter(description = "Id do item", example = "1") @PathVariable Long id) {
        itemService.remover(id);
        return ResponseEntity.noContent().build();
    }

    private List<ItemResponseDTO> paraDTO(List<Item> itens) {
        return itens.stream().map(ItemResponseDTO::fromEntity).toList();
    }
}
