package org.example.trusttrade.order.dto;

import lombok.Getter;
import org.example.trusttrade.order.domain.Payment;

@Getter
public class PaymentReadyResponse {
    private Long paymentId;
    private String idempotencyKey;
    private Payment.Status status;

    public PaymentReadyResponse(Long paymentId, String idempotencyKey, Payment.Status status) {
        this.paymentId = paymentId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }
}
