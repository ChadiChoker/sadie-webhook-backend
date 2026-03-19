package com.sadie.sadie_webhook_backend.controller;

import com.sadie.sadie_webhook_backend.dto.api.CallDetailDto;
import com.sadie.sadie_webhook_backend.dto.api.CallSessionDto;
import com.sadie.sadie_webhook_backend.service.CallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/beta/api/calls")
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @GetMapping
    public ResponseEntity<List<CallSessionDto>> listCalls() {
        return ResponseEntity.ok(callService.listSessions());
    }

    @GetMapping("/{callId}")
    public ResponseEntity<CallDetailDto> getCallDetail(@PathVariable String callId) {
        return ResponseEntity.ok(callService.getDetail(callId));
    }
}
