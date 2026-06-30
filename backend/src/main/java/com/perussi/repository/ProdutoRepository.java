package com.perussi.repository;

import com.perussi.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    List<Produto> findByCategoriaIgnoreCase(String categoria);

    List<Produto> findByDestaqueTrue();
}