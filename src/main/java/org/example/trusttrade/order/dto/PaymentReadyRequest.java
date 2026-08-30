package org.example.trusttrade.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Setter @Getter
public class PaymentReadyRequest {
    private String orderId;
    private String idempotencyKey;

}
