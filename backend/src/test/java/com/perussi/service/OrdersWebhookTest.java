package com.perussi.service;

import com.perussi.security.WebhookSignatureVerifier;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrdersWebhookTest {
    @Test void signedOrderIsDispatchedAndTamperingIsRejected() throws Exception {
        var service = mock(PagamentoService.class);
        var webhook = new WebhookService(service);
        var verifier = new WebhookSignatureVerifier("legacy-secret", "orders-secret");
        Map<String,Object> payload = Map.of("type", "order", "data", Map.of("id", "ORD01TEST"));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("orders-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "ts=1742505638683,v1=" + HexFormat.of().formatHex(mac.doFinal(
                "id:ORD01TEST;request-id:req;ts:1742505638683;".getBytes(StandardCharsets.UTF_8)));
        verifier.verify(signature, "req", "ORD01TEST", payload);
        webhook.processarWebhook(payload);
        verify(service).sincronizar("ORD01TEST");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> verifier.verify(signature, "req", "ORD01TEST", Map.of("data", Map.of("id", "ORD02OTHER"))));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new WebhookSignatureVerifier("orders-secret", "wrong-secret").verify(signature, "req", "ORD01TEST", payload));
    }
}
