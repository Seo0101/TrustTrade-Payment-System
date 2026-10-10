package org.example.trusttrade.order.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.order.exception.SystemException.PaymentSystemException;
import org.springframework.stereotype.Component;

@Component
public class PaymentResponseParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode parse(String body) {
        try {
            JsonNode root = mapper.readTree(body);

            if (root == null || !root.isObject()) {
                throw new PaymentSystemException(
                        ErrorCode.PAYMENT_PROVIDER_ERROR
                );
            }

            return root;

        } catch (JsonProcessingException e) {
            throw new PaymentSystemException(
                    ErrorCode.PAYMENT_PROVIDER_ERROR
            );
        }
    }

    public String extractStatus(String body) {
        return requiredText(parse(body), "status");
    }

    public String extractErrorCode(String body) {
        return requiredText(parse(body), "code");
    }

    public String extractErrorMessage(String body) {
        return requiredText(parse(body), "message");
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);

        if (node == null
                || !node.isTextual()
                || node.asText().isBlank()) {
            throw new PaymentSystemException(
                    ErrorCode.PAYMENT_PROVIDER_ERROR
            );
        }

        return node.asText();
    }
}
