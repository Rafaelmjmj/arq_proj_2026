package com.trokr.repository;

import com.trokr.model.CategoriaItem;
import com.trokr.model.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByTituloContainingIgnoreCase(String titulo);

    List<Item> findByCategoria(CategoriaItem categoria);

    List<Item> findByUsuarioProprietarioId(Long usuarioId);
}
