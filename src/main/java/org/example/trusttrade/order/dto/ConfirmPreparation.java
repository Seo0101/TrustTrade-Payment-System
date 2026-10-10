package org.example.trusttrade.order.dto;

public record ConfirmPreparation(
        boolean requestApproval,
        String orderId,
        int amount,
        String paymentKey,
        String idempotencyKey
) {
}
