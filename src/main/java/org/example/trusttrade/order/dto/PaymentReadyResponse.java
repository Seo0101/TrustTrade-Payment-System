package org.example.trusttrade.order.dto;

import lombok.Getter;

@Getter
public class PaymentReadyResponse {
    private Long paymentId;
    private String idempotencyKey;

    public PaymentReadyResponse(Long paymentId, String idempotencyKey) {
        this.paymentId = paymentId;
        this.idempotencyKey = idempotencyKey;
    }
}
