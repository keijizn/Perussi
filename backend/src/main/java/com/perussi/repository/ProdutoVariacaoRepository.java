package com.perussi.repository;

import com.perussi.model.ProdutoVariacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ProdutoVariacaoRepository extends JpaRepository<ProdutoVariacao, UUID> {

    List<ProdutoVariacao> findByProdutoId(UUID produtoId);

    Optional<ProdutoVariacao> findByIdAndProdutoId(
            UUID variacaoId,
            UUID produtoId
    );
}