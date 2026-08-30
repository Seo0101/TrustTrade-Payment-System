package org.example.trusttrade.order.exception;

import java.io.Serializable;

public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
