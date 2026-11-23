package com.trokr.repository;

import com.trokr.model.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório da hierarquia inteira (Aula 12): as consultas devolvem Item e
 * cada linha volta como a subclasse certa (ItemProduto, ItemServico,
 * ItemExperiencia), escolhida pelo discriminador. Nenhuma consulta mudou por
 * causa da herança — essa é uma das vantagens de SINGLE_TABLE.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByTituloContainingIgnoreCase(String titulo);

    /**
     * A categoria deixou de ser um campo da entidade e virou o discriminador
     * da herança, então "findByCategoria" não existe mais como query method.
     * Filtramos direto pela coluna discriminadora.
     */
    @Query(value = "select * from item where categoria = :categoria", nativeQuery = true)
    List<Item> findByCategoria(@Param("categoria") String categoria);

    List<Item> findByUsuarioProprietarioId(Long usuarioId);
}
