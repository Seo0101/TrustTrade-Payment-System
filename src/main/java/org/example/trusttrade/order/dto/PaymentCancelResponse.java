package org.example.trusttrade.order.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCancelResponse {
    private String status;
    private String cancelReason;
    private int cancelAmount;
}