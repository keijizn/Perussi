package com.perussi.repository;

import com.perussi.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByUsuarioId(UUID usuarioId);

    Optional<Pedido> findByMercadoPagoPaymentId(
            String mercadoPagoPaymentId
    );
}