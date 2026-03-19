package com.sadie.sadie_webhook_backend.service.impl;

import com.sadie.sadie_webhook_backend.dto.webhook.WebhookEventRequest;
import com.sadie.sadie_webhook_backend.entity.CallMessage;
import com.sadie.sadie_webhook_backend.entity.CallSession;
import com.sadie.sadie_webhook_backend.entity.CallStatus;
import com.sadie.sadie_webhook_backend.repository.CallMessageRepository;
import com.sadie.sadie_webhook_backend.repository.CallSessionRepository;
import com.sadie.sadie_webhook_backend.repository.ToolCallLogRepository;
import com.sadie.sadie_webhook_backend.service.WebhookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookServiceImpl implements WebhookService {

    private final CallSessionRepository callSessionRepository;
    private final CallMessageRepository callMessageRepository;
    private final ToolCallLogRepository toolCallLogRepository;

    public WebhookServiceImpl(CallSessionRepository callSessionRepository,
                               CallMessageRepository callMessageRepository,
                               ToolCallLogRepository toolCallLogRepository) {
        this.callSessionRepository = callSessionRepository;
        this.callMessageRepository = callMessageRepository;
        this.toolCallLogRepository = toolCallLogRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> handle(WebhookEventRequest request) {
        if ("assistant-request".equals(request.getType())) {
            return handleAssistantRequest(request);
        }
        if ("end-of-call-report".equals(request.getType())) {
            return handleEndOfCallReport(request);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", "Unknown event type: " + request.getType());
        return response;
    }

    private Map<String, Object> handleAssistantRequest(WebhookEventRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();

        // Idempotency check — if session already exists, do nothing
        if (callSessionRepository.existsByCallId(request.getCallId())) {
            response.put("success", true);
            response.put("message", "Session already exists");
            return response;
        }

        try {
            CallSession session = new CallSession();
            session.setCallId(request.getCallId());
            session.setTenantId(request.getTenantId() != null ? request.getTenantId() : "default");
            session.setAssistantId(request.getAssistantId());
            session.setCustomerNumber(request.getCustomerNumber());
            session.setDealershipNumber(request.getRestaurantNumber());
            session.setStatus(CallStatus.IN_PROGRESS);
            session.setStartedAt(OffsetDateTime.now());

            callSessionRepository.save(session);

            response.put("success", true);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the session concurrently — treat as no-op
            response.put("success", true);
            response.put("message", "Session already exists");
        }

        return response;
    }

    private Map<String, Object> handleEndOfCallReport(WebhookEventRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();

        // Find existing session or create one defensively
        CallSession session = callSessionRepository.findByCallId(request.getCallId())
                .orElseGet(() -> {
                    CallSession s = new CallSession();
                    s.setCallId(request.getCallId());
                    s.setTenantId(request.getTenantId() != null ? request.getTenantId() : "default");
                    s.setAssistantId(request.getAssistantId());
                    s.setCustomerNumber(request.getCustomerNumber());
                    s.setDealershipNumber(request.getRestaurantNumber());
                    return s;
                });

        // Update session fields
        session.setStatus(CallStatus.COMPLETED);
        session.setTranscript(request.getTranscript());
        session.setSummary(request.getSummary());
        session.setRecordingUrl(request.getRecordingUrl());
        session.setCategory(request.getCategory());
        session.setEndedReason(request.getEndedReason());

        if (request.getDurationSeconds() != null) {
            session.setDurationSeconds(BigDecimal.valueOf(request.getDurationSeconds()));
        }
        if (request.getDurationMinutes() != null) {
            session.setDurationMinutes(BigDecimal.valueOf(request.getDurationMinutes()));
        }
        if (request.getStartedAt() != null) {
            session.setStartedAt(OffsetDateTime.parse(request.getStartedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (request.getEndedAt() != null) {
            session.setEndedAt(OffsetDateTime.parse(request.getEndedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        callSessionRepository.save(session);

        // Save messages
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            List<CallMessage> messages = new ArrayList<>();
            for (WebhookEventRequest.MessagePayload mp : request.getMessages()) {
                CallMessage msg = new CallMessage();
                msg.setSession(session);
                msg.setTenantId(session.getTenantId());
                msg.setRole(mp.getRole());
                msg.setMessage(mp.getMessage());
                if (mp.getTime() != null)               msg.setTime(BigDecimal.valueOf(mp.getTime()));
                if (mp.getEndTime() != null)            msg.setEndTime(BigDecimal.valueOf(mp.getEndTime()));
                if (mp.getSecondsFromStart() != null)   msg.setSecondsFromStart(BigDecimal.valueOf(mp.getSecondsFromStart()));
                if (mp.getDuration() != null)           msg.setDuration(BigDecimal.valueOf(mp.getDuration()));
                messages.add(msg);
            }
            callMessageRepository.saveAll(messages);
        }

        // Retroactively link any tool logs that arrived before this end-of-call-report
        toolCallLogRepository.linkToSession(session, request.getCallId());

        response.put("success", true);
        return response;
    }
}
