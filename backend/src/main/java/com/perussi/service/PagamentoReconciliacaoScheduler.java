package com.perussi.service;

import com.perussi.repository.PedidoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PagamentoReconciliacaoScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PagamentoReconciliacaoScheduler.class);
    private final PedidoRepository pedidos;
    private final PagamentoService pagamentos;

    public PagamentoReconciliacaoScheduler(PedidoRepository pedidos, PagamentoService pagamentos) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
    }

    @Scheduled(fixedDelayString = "${pagamentos.conciliacao.intervalo-ms:60000}",
            initialDelayString = "${pagamentos.conciliacao.intervalo-ms:60000}")
    public void reconciliar() {
        // Snapshot IDs, then one short transaction per order. Never release stock on API errors.
        for (var id : pedidos.findPagamentosPendentes()) {
            try { pagamentos.reconciliarPendente(id); }
            catch (RuntimeException e) {
                log.warn("Conciliacao pendente para pedido {}; nova tentativa no proximo ciclo", id);
            }
        }
    }
}
