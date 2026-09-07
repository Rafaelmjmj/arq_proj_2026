package com.trokr.repository;

import com.trokr.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByTitulo(String titulo);

    Optional<Item> findByDescricao(String descricao);

    List<Item> findByTituloContaining(String titulo);

    List<Item> findByDescricaoContaining(String descricao);

    List<Item> findByUsuarioProprietarioId(Long usuarioId);
}