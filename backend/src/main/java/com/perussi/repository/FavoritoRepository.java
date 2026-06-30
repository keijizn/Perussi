package com.perussi.repository;

import com.perussi.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    List<Favorito> findByUsuarioId(UUID usuarioId);

    Optional<Favorito> findByUsuarioIdAndProdutoId(UUID usuarioId, UUID produtoId);

    boolean existsByUsuarioIdAndProdutoId(UUID usuarioId, UUID produtoId);

    void deleteByUsuarioIdAndProdutoId(UUID usuarioId, UUID produtoId);
}