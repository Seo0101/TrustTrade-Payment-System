package org.example.trusttrade.order.exception.SystemException;

import jakarta.transaction.SystemException;

public class DuplicateOrderException extends RuntimeException {

    private final String orderLockKey;

    public DuplicateOrderException(String orderLockKey) {
        this.orderLockKey = orderLockKey;
    }
}
