package org.example.trusttrade.order.exception.SystemException;

public class PaymentCreationFailedException extends RuntimeException {
    public PaymentCreationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
