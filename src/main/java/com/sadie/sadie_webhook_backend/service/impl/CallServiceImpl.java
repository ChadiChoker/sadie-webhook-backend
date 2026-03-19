package com.sadie.sadie_webhook_backend.service.impl;

import com.sadie.sadie_webhook_backend.dto.api.CallDetailDto;
import com.sadie.sadie_webhook_backend.dto.api.CallMessageDto;
import com.sadie.sadie_webhook_backend.dto.api.CallSessionDto;
import com.sadie.sadie_webhook_backend.dto.api.ToolCallLogDto;
import com.sadie.sadie_webhook_backend.entity.CallSession;
import com.sadie.sadie_webhook_backend.repository.CallMessageRepository;
import com.sadie.sadie_webhook_backend.repository.CallSessionRepository;
import com.sadie.sadie_webhook_backend.repository.ToolCallLogRepository;
import com.sadie.sadie_webhook_backend.service.CallService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CallServiceImpl implements CallService {

    private final CallSessionRepository callSessionRepository;
    private final CallMessageRepository callMessageRepository;
    private final ToolCallLogRepository toolCallLogRepository;

    public CallServiceImpl(CallSessionRepository callSessionRepository,
                           CallMessageRepository callMessageRepository,
                           ToolCallLogRepository toolCallLogRepository) {
        this.callSessionRepository = callSessionRepository;
        this.callMessageRepository = callMessageRepository;
        this.toolCallLogRepository = toolCallLogRepository;
    }

    @Override
    public List<CallSessionDto> listSessions() {
        return callSessionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(CallSessionDto::new)
                .toList();
    }

    @Override
    public CallDetailDto getDetail(String callId) {
        CallSession session = callSessionRepository.findByCallId(callId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found: " + callId));

        List<CallMessageDto> messages = callMessageRepository
                .findBySessionIdOrderBySecondsFromStartAsc(session.getId())
                .stream()
                .map(CallMessageDto::new)
                .toList();

        List<ToolCallLogDto> toolLogs = toolCallLogRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(ToolCallLogDto::new)
                .toList();

        return new CallDetailDto(session, messages, toolLogs);
    }
}
