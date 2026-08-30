package org.example.trusttrade.order.exception;

public class OrderCancellationException extends RuntimeException {

    private final String errorCode;

    public OrderCancellationException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

