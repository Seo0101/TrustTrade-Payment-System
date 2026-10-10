package org.example.trusttrade.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Setter @Getter
public class PaymentReadyRequest {

    @NotBlank
    private String orderId;
    @NotBlank
    private String idempotencyKey;

}
