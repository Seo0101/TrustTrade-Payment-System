package org.example.trusttrade.order.exception;

public class PaymentNotFoundException extends NotFoundException {
    public PaymentNotFoundException(String paymentId) {
        super("결제를 찾을 수 없습니다. orderId=" + paymentId);
    }
}
