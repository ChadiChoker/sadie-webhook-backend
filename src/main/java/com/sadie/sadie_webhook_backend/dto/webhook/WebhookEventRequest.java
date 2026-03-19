package com.sadie.sadie_webhook_backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEventRequest {

    private String type;
    private Long timestamp;
    private String status;

    @JsonProperty("customer_number")
    private String customerNumber;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("assistant_id")
    private String assistantId;

    @JsonProperty("call_id")
    private String callId;

    @JsonProperty("restaurant_number")
    private String restaurantNumber;

    // end-of-call-report fields
    private String transcript;
    private String summary;

    @JsonProperty("recording_url")
    private String recordingUrl;

    @JsonProperty("duration_seconds")
    private Double durationSeconds;

    @JsonProperty("duration_minutes")
    private Double durationMinutes;

    private String category;

    @JsonProperty("ended_reason")
    private String endedReason;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("ended_at")
    private String endedAt;

    private List<MessagePayload> messages;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessagePayload {
        private String role;
        private String message;
        private Long time;
        private Long endTime;
        private Double secondsFromStart;
        private Long duration;
    }
}
