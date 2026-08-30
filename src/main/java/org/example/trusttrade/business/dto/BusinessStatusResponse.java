package org.example.trusttrade.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class BusinessStatusResponse {

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("status_message")
    private String statusMessage;

    private List<Map<String, Object>> data;
}