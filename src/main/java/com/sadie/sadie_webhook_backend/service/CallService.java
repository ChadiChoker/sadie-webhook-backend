package com.sadie.sadie_webhook_backend.service;

import com.sadie.sadie_webhook_backend.dto.api.CallDetailDto;
import com.sadie.sadie_webhook_backend.dto.api.CallSessionDto;

import java.util.List;

public interface CallService {
    List<CallSessionDto> listSessions();
    CallDetailDto getDetail(String callId);
}
