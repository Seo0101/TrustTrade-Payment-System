package org.example.trusttrade.global.error;

public class NotAllowUserType extends RuntimeException {
    public NotAllowUserType (String message) {
        super(message);
    }
}
