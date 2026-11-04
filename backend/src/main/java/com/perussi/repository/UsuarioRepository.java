package com.perussi.repository;

import com.perussi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> findLockedByEmail(@org.springframework.data.repository.query.Param("email") String email);
    Optional<Usuario> findByEmail(String email);
    @org.springframework.data.jpa.repository.Query("select u.id from Usuario u where u.recuperacaoHash = :hash")
    Optional<UUID> findIdByRecuperacaoHash(@org.springframework.data.repository.query.Param("hash") String hash);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
