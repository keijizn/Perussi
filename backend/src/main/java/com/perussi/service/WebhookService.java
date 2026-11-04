package com.perussi.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class WebhookService {
    private final PagamentoService pagamentos;
    public WebhookService(PagamentoService pagamentos) { this.pagamentos = pagamentos; }

    public void processarWebhook(Map<String, Object> payload) {
        if (!(payload.get("data") instanceof Map<?, ?> data) || data.get("id") == null) return;
        String id = String.valueOf(data.get("id"));
        boolean payment = id.matches("[0-9]+") && (payload.get("type") == null || "payment".equals(payload.get("type")));
        boolean order = id.matches("ORD[A-Z0-9]+") && "order".equals(payload.get("type"));
        if (!payment && !order) return;
        // Let failures reach HTTP: unsuccessful processing must not be acknowledged as success.
        pagamentos.sincronizar(id);
    }
}
