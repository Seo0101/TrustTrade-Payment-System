package org.example.trusttrade.order.exception.SystemException;

public class DuplicatePaymentException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicatePaymentException(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
