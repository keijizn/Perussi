package com.perussi.service;

import com.perussi.repository.PedidoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@Component
public class ReservaScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReservaScheduler.class);
    private final PedidoRepository pedidos;
    private final PagamentoService pagamentos;
    public ReservaScheduler(PedidoRepository pedidos, PagamentoService pagamentos) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
    }

    @Scheduled(fixedDelayString = "${pedidos.reserva.verificacao-ms:60000}")
    public void expirar() {
        for (var id : pedidos.findExpirados(LocalDateTime.now())) {
            try { pagamentos.expirar(id); }
            catch (RuntimeException exception) {
                log.warn("Reserva do pedido {} mantida; conciliação será repetida: {}", id, exception.getMessage());
            }
        }
    }
}
