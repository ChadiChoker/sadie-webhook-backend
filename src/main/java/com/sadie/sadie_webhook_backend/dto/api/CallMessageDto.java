package com.sadie.sadie_webhook_backend.dto.api;

import com.sadie.sadie_webhook_backend.entity.CallMessage;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class CallMessageDto {

    private final UUID id;
    private final String role;
    private final String message;
    private final BigDecimal secondsFromStart;
    private final BigDecimal duration;

    public CallMessageDto(CallMessage m) {
        this.id                = m.getId();
        this.role              = m.getRole();
        this.message           = m.getMessage();
        this.secondsFromStart  = m.getSecondsFromStart();
        this.duration          = m.getDuration();
    }
}
