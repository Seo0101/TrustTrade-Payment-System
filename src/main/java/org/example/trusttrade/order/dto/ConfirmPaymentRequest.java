package org.example.trusttrade.order.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentRequest {
    private int amount;
    private String orderId;
    private String paymentKey;
    private String idempotencyKey;
}
