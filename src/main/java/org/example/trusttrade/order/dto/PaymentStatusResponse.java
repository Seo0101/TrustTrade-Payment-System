package org.example.trusttrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.trusttrade.order.domain.Payment;

@Getter
@AllArgsConstructor
public class PaymentStatusResponse {
    private String orderId;
    private Payment.Status status;
    private int amount;
    private String message;


    public static PaymentStatusResponse from(Payment payment) {

        String message = switch (payment.getStatus()) {
            case SUCCESS -> "결제가 완료되었습니다.";
            case FAILED -> "결제가 실패했습니다.";
            case CONFIRMING -> "결제 처리 중입니다.";
            default -> "결제 상태를 확인하고 있습니다.";
        };

        return new PaymentStatusResponse(
                payment.getOrder().getId(),
                payment.getStatus(),
                payment.getAmount(),
                message
        );
    }
}