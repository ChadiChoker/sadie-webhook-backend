package com.sadie.sadie_webhook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "call_messages")
@Getter
@Setter
public class CallMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CallSession session;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(precision = 20, scale = 4)
    private BigDecimal time;

    @Column(name = "end_time", precision = 20, scale = 4)
    private BigDecimal endTime;

    @Column(name = "seconds_from_start", precision = 10, scale = 4)
    private BigDecimal secondsFromStart;

    @Column(precision = 10, scale = 4)
    private BigDecimal duration;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
