package org.example.trusttrade.order.exception;

import org.example.trusttrade.order.domain.Order;


public class PaymentAmountMismatchException extends RuntimeException {

    private final Order order;
    public PaymentAmountMismatchException(Order order) {
        super("결제 금액이 주문 금액과 일치하지 않습니다.");
        this.order = order;
    }
    public Order getOrder() {
        return order;
    }
}
