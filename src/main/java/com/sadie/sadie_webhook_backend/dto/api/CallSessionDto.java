package com.sadie.sadie_webhook_backend.dto.api;

import com.sadie.sadie_webhook_backend.entity.CallSession;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class CallSessionDto {

    private final UUID id;
    private final String callId;
    private final String tenantId;
    private final String assistantId;
    private final String customerNumber;
    private final String dealershipNumber;
    private final String status;
    private final String category;
    private final String endedReason;
    private final BigDecimal durationSeconds;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime endedAt;
    private final OffsetDateTime createdAt;

    public CallSessionDto(CallSession s) {
        this.id               = s.getId();
        this.callId           = s.getCallId();
        this.tenantId         = s.getTenantId();
        this.assistantId      = s.getAssistantId();
        this.customerNumber   = s.getCustomerNumber();
        this.dealershipNumber = s.getDealershipNumber();
        this.status           = s.getStatus().name();
        this.category         = s.getCategory();
        this.endedReason      = s.getEndedReason();
        this.durationSeconds  = s.getDurationSeconds();
        this.startedAt        = s.getStartedAt();
        this.endedAt          = s.getEndedAt();
        this.createdAt        = s.getCreatedAt();
    }
}
