package com.perussi.service;

import com.perussi.model.Pedido;
import java.math.BigDecimal;
import java.util.Optional;

public interface PixGateway {
    record Pagamento(String id, String status, BigDecimal total, String referencia,
                     String qrCode, String qrCodeBase64) {}
    Pagamento criar(Pedido pedido);
    default void preparar(Pedido pedido) {}
    default Optional<Pagamento> recuperar(Pedido pedido) { return recuperar(pedido.getId().toString()); }
    Pagamento consultar(String id);
    Pagamento cancelar(String id);
    Optional<Pagamento> recuperar(String referencia);
}
