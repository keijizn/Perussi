package com.perussi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Component
public class WebhookSignatureVerifier {
    private final String secret;
    private final String ordersSecret;

    @org.springframework.beans.factory.annotation.Autowired
    public WebhookSignatureVerifier(@Value("${mercadopago.webhook-secret:}") String secret,
            @Value("${mercadopago.orders-webhook-secret:}") String ordersSecret) {
        this.secret = secret;
        this.ordersSecret = ordersSecret;
    }

    public WebhookSignatureVerifier(String secret) { this(secret, ""); }

    public void verify(String signature, String requestId, String dataId, Map<String, Object> payload) {
        String signingSecret = dataId != null && dataId.startsWith("ORD") ? ordersSecret : secret;
        if (signingSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notificacoes indisponiveis");
        if (signature == null || requestId == null || requestId.isBlank()
                || requestId.contains(";") || dataId == null || !dataId.matches("(?:[0-9]+|ORD[A-Z0-9]+)")) reject();
        // Bind the processed payment to the signed query parameter, never to unsigned body data.
        if (!(payload.get("data") instanceof Map<?, ?> data)
                || !dataId.equals(String.valueOf(data.get("id")))) reject();
        Map<String, String> parts = new HashMap<>();
        for (String part : signature.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2 || parts.putIfAbsent(pair[0].trim(), pair[1].trim()) != null) reject();
        }
        String ts = parts.getOrDefault("ts", "");
        String hash = parts.getOrDefault("v1", "");
        if (!ts.matches("[0-9]+") || !hash.matches("[a-fA-F0-9]{64}")) reject();
        String manifest = "id:" + dataId + ";request-id:" + requestId.trim() + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)),
                    HexFormat.of().parseHex(hash))) reject();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponivel", e);
        }
        // No age cutoff: provider retries may be delayed. Processing re-queries current remote
        // state and is idempotent, so a valid replay cannot restore a previous payment state.
    }

    private static void reject() { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Notificacao nao autorizada"); }
}
