package org.example.trusttrade.order.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentResponse {

    private String orderId;
    private String paymentKey;
    private String status;
    private String message;
}
