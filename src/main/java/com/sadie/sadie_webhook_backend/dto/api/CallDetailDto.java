package com.sadie.sadie_webhook_backend.dto.api;

import com.sadie.sadie_webhook_backend.entity.CallSession;
import lombok.Getter;

import java.util.List;

@Getter
public class CallDetailDto extends CallSessionDto {

    private final String transcript;
    private final String summary;
    private final String recordingUrl;
    private final List<CallMessageDto> messages;
    private final List<ToolCallLogDto> toolLogs;

    public CallDetailDto(CallSession s, List<CallMessageDto> messages, List<ToolCallLogDto> toolLogs) {
        super(s);
        this.transcript  = s.getTranscript();
        this.summary     = s.getSummary();
        this.recordingUrl = s.getRecordingUrl();
        this.messages    = messages;
        this.toolLogs    = toolLogs;
    }
}
