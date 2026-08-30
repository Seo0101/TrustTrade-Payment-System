package org.example.trusttrade.order.exception;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(String massage)  {
        super(massage);
    }
}
