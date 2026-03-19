package com.sadie.sadie_webhook_backend.dto.api;

import com.sadie.sadie_webhook_backend.entity.ToolCallLog;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
public class ToolCallLogDto {

    private final UUID id;
    private final String toolName;
    private final String callId;
    private final Map<String, Object> requestJson;
    private final Map<String, Object> responseJson;
    private final OffsetDateTime createdAt;

    public ToolCallLogDto(ToolCallLog t) {
        this.id           = t.getId();
        this.toolName     = t.getToolName();
        this.callId       = t.getCallId();
        this.requestJson  = t.getRequestJson();
        this.responseJson = t.getResponseJson();
        this.createdAt    = t.getCreatedAt();
    }
}
