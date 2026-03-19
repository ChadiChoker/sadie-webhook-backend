package com.sadie.sadie_webhook_backend.service;

import com.sadie.sadie_webhook_backend.dto.tool.ToolRequest;

import java.util.Map;

public interface ToolService {
    Map<String, Object> searchAvailability(ToolRequest request);
    Map<String, Object> createReservation(ToolRequest request);
}
