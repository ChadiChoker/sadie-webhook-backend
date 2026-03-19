package com.sadie.sadie_webhook_backend.service;

import com.sadie.sadie_webhook_backend.dto.webhook.WebhookEventRequest;

import java.util.Map;

public interface WebhookService {
    Map<String, Object> handle(WebhookEventRequest request);
}
