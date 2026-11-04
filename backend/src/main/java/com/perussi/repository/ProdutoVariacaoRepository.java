package com.perussi.repository;

import com.perussi.model.ProdutoVariacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ProdutoVariacaoRepository extends JpaRepository<ProdutoVariacao, UUID> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update ProdutoVariacao v set v.estoque = v.estoque - :quantidade where v.id = :id and v.estoque >= :quantidade and :quantidade > 0")
    int reservar(@org.springframework.data.repository.query.Param("id") UUID id,
                 @org.springframework.data.repository.query.Param("quantidade") int quantidade);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update ProdutoVariacao v set v.estoque = v.estoque + :quantidade where v.id = :id")
    int liberar(@org.springframework.data.repository.query.Param("id") UUID id,
                @org.springframework.data.repository.query.Param("quantidade") int quantidade);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProdutoVariacao v where v.id = :id")
    Optional<ProdutoVariacao> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<ProdutoVariacao> findByProdutoId(UUID produtoId);

    Optional<ProdutoVariacao> findByIdAndProdutoId(
            UUID variacaoId,
            UUID produtoId
    );
}
