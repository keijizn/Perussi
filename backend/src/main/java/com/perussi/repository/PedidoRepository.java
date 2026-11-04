package com.perussi.repository;

import com.perussi.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("select p.id from Pedido p where p.status = com.perussi.model.StatusPedido.PENDENTE and p.reservaExpiraEm <= :agora order by p.reservaExpiraEm")
    List<UUID> findExpirados(@org.springframework.data.repository.query.Param("agora") java.time.LocalDateTime agora);

    List<Pedido> findByUsuarioId(UUID usuarioId);

    @org.springframework.data.jpa.repository.Query("select p.id from Pedido p where p.status = com.perussi.model.StatusPedido.PENDENTE and p.mercadoPagoPaymentId is not null order by p.criadoEm, p.id")
    List<UUID> findPagamentosPendentes();

    @org.springframework.data.jpa.repository.Query("select p.id from Pedido p where p.mercadoPagoPaymentId = :paymentId")
    Optional<UUID> findIdByPaymentId(@org.springframework.data.repository.query.Param("paymentId") String paymentId);

    Optional<Pedido> findByMercadoPagoPaymentId(
            String mercadoPagoPaymentId
    );
}
