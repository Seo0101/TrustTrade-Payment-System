package org.example.trusttrade.order.exception;

public class OrderStateUpdateException extends RuntimeException{

    public OrderStateUpdateException(String message) {
        super(message);
    }
}
