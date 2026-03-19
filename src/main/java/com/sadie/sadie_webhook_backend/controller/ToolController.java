package com.sadie.sadie_webhook_backend.controller;

import com.sadie.sadie_webhook_backend.dto.tool.ToolRequest;
import com.sadie.sadie_webhook_backend.service.ToolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/beta/api/sadie/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @PostMapping("/search-availability")
    public ResponseEntity<Map<String, Object>> searchAvailability(@RequestBody ToolRequest request) {
        return ResponseEntity.ok(toolService.searchAvailability(request));
    }

    @PostMapping("/create-reservation")
    public ResponseEntity<Map<String, Object>> createReservation(@RequestBody ToolRequest request) {
        return ResponseEntity.ok(toolService.createReservation(request));
    }
}
