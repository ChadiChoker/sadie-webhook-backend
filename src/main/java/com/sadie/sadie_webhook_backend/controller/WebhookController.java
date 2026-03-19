package com.sadie.sadie_webhook_backend.controller;

import com.sadie.sadie_webhook_backend.dto.webhook.WebhookEventRequest;
import com.sadie.sadie_webhook_backend.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/beta/api/sadie")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody WebhookEventRequest body) {
        return ResponseEntity.ok(webhookService.handle(body));
    }
}
