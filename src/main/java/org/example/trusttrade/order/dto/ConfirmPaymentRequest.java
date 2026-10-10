package org.example.trusttrade.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentRequest {

    //양수 검증
    @Positive
    private int amount;

    @NotBlank
    private String orderId;

    @NotBlank
    @Size(max = 200)
    private String paymentKey;

    @NotBlank
    private String idempotencyKey;
}
