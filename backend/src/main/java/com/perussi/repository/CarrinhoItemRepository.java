package com.perussi.repository;

import com.perussi.model.CarrinhoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrinhoItemRepository extends JpaRepository<CarrinhoItem, UUID> {

    List<CarrinhoItem> findByUsuarioId(UUID usuarioId);

    Optional<CarrinhoItem> findByIdAndUsuarioId(UUID itemId, UUID usuarioId);

    Optional<CarrinhoItem> findByUsuarioIdAndProdutoIdAndVariacaoId(
            UUID usuarioId,
            UUID produtoId,
            UUID variacaoId
    );
}