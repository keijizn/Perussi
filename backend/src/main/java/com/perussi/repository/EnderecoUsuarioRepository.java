package com.perussi.repository;

import com.perussi.model.EnderecoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnderecoUsuarioRepository extends JpaRepository<EnderecoUsuario, UUID> {
    Optional<EnderecoUsuario> findByUsuarioId(UUID usuarioId);
}