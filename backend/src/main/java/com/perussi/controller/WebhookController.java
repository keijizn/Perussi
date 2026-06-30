package com.perussi.controller;

import com.perussi.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receberWebhook(
            @RequestBody Map<String, Object> payload
    ) {
        webhookService.processarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}