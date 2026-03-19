package com.sadie.sadie_webhook_backend.service.impl;

import com.sadie.sadie_webhook_backend.dto.tool.ToolRequest;
import com.sadie.sadie_webhook_backend.entity.CallSession;
import com.sadie.sadie_webhook_backend.entity.ToolCallLog;
import com.sadie.sadie_webhook_backend.repository.CallSessionRepository;
import com.sadie.sadie_webhook_backend.repository.ToolCallLogRepository;
import com.sadie.sadie_webhook_backend.service.ToolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ToolServiceImpl implements ToolService {

    private final ToolCallLogRepository toolCallLogRepository;
    private final CallSessionRepository callSessionRepository;

    public ToolServiceImpl(ToolCallLogRepository toolCallLogRepository,
                           CallSessionRepository callSessionRepository) {
        this.toolCallLogRepository = toolCallLogRepository;
        this.callSessionRepository = callSessionRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> searchAvailability(ToolRequest request) {
        try {
            ToolCallLog log = createLog("search-availability", request);

            Map<String, Object> response = buildSearchAvailabilityResponse();

            log.setResponseJson(response);
            toolCallLogRepository.save(log);

            return response;
        } catch (Exception e) {
            return errorResponse("search-availability failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createReservation(ToolRequest request) {
        try {
            ToolCallLog log = createLog("create-reservation", request);

            Map<String, Object> args = request.getArguments();
            boolean missingFirstName = args == null || !args.containsKey("firstName") || args.get("firstName") == null;
            boolean missingLastName  = args == null || !args.containsKey("lastName")  || args.get("lastName")  == null;

            Map<String, Object> response;
            if (missingFirstName || missingLastName) {
                response = buildMissingFieldsResponse();
            } else {
                response = buildCreateReservationResponse();
            }

            log.setResponseJson(response);
            toolCallLogRepository.save(log);

            return response;
        } catch (Exception e) {
            return errorResponse("create-reservation failed: " + e.getMessage());
        }
    }

    // --- helpers ---

    private ToolCallLog createLog(String toolName, ToolRequest request) {
        ToolCallLog log = new ToolCallLog();
        log.setToolName(toolName);
        log.setCallId(request.getCallId());
        log.setTenantId("default");

        Map<String, Object> requestJson = new LinkedHashMap<>();
        requestJson.put("call_id", request.getCallId());
        requestJson.put("arguments", request.getArguments());
        log.setRequestJson(requestJson);

        // Link to session if it exists
        if (request.getCallId() != null) {
            callSessionRepository.findByCallId(request.getCallId())
                    .ifPresent(session -> {
                        log.setSession(session);
                        log.setTenantId(session.getTenantId());
                    });
        }

        toolCallLogRepository.save(log);
        return log;
    }

    private Map<String, Object> buildSearchAvailabilityResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);

        Map<String, Object> economy = new LinkedHashMap<>();
        economy.put("vehicleClassId", "cls_economy");
        economy.put("className", "Economy");
        economy.put("pricePerDay", 39.00);
        economy.put("totalPrice", 117.00);

        Map<String, Object> midsize = new LinkedHashMap<>();
        midsize.put("vehicleClassId", "cls_midsize");
        midsize.put("className", "Mid-Size");
        midsize.put("pricePerDay", 59.00);
        midsize.put("totalPrice", 177.00);

        Map<String, Object> suv = new LinkedHashMap<>();
        suv.put("vehicleClassId", "cls_suv");
        suv.put("className", "SUV");
        suv.put("pricePerDay", 89.00);
        suv.put("totalPrice", 267.00);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", true);
        data.put("vehicles", List.of(economy, midsize, suv));

        response.put("data", data);
        response.put("description", "3 vehicle classes available for the requested dates");
        response.put("steps", List.of(
                "Present the 3 available options with prices",
                "Ask the caller which vehicle class they prefer",
                "Confirm their selection before proceeding"
        ));

        return response;
    }

    private Map<String, Object> buildCreateReservationResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("confirmationNumber", "87430618");
        data.put("status", "CONFIRMED");
        data.put("totalPrice", 177.00);
        data.put("currency", "USD");

        response.put("data", data);
        response.put("description", "Reservation confirmed for John Smith");
        response.put("steps", List.of(
                "Tell the caller their reservation is confirmed",
                "Give them confirmation number 87430618",
                "Let them know a payment link will be sent by SMS",
                "Ask if there is anything else you can help with"
        ));

        return response;
    }

    private Map<String, Object> buildMissingFieldsResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("error", "Missing required fields: firstName, lastName");

        response.put("data", data);
        response.put("description", "Cannot create reservation without customer name");
        response.put("steps", List.of(
                "Politely ask the caller for their first and last name",
                "Retry the reservation with the complete information"
        ));

        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("error", message);

        response.put("data", data);
        response.put("description", "An internal error occurred");
        response.put("steps", List.of("Please try again"));

        return response;
    }
}
