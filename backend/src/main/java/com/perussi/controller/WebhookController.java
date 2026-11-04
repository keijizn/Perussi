package com.perussi.controller;

import com.perussi.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final com.perussi.security.WebhookSignatureVerifier signatures;

    public WebhookController(WebhookService webhookService, com.perussi.security.WebhookSignatureVerifier signatures) {
        this.webhookService = webhookService;
        this.signatures = signatures;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receberWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId
    ) {
        signatures.verify(signature, requestId, dataId, payload);
        webhookService.processarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
