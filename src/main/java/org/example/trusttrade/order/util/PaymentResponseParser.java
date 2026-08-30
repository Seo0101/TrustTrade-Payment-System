package org.example.trusttrade.order.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PaymentResponseParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public String extractStatus(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode statusNode = root.get("status");

            return statusNode != null ? statusNode.asText() : null;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String extractErrorCode(String body){
        try{
            JsonNode root = mapper.readTree(body);
            JsonNode errorNode = root.get("error");

            return errorNode != null ? errorNode.get("code").asText() : null;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String extractErrorMessage(String body){
        try{
            JsonNode root = mapper.readTree(body);
            JsonNode errorMessage = root.get("message");

            return errorMessage != null ? errorMessage.get("message").asText() : null;
        }catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
