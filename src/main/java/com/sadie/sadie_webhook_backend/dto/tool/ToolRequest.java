package com.sadie.sadie_webhook_backend.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolRequest {

    @JsonProperty("call_id")
    private String callId;

    private Map<String, Object> arguments;
}
