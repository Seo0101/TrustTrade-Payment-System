package org.example.trusttrade.order.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentErrorResponse {
    private int code;
    private String message;
}
