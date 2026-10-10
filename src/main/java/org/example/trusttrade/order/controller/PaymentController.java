package org.example.trusttrade.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.dto.*;
import org.example.trusttrade.order.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    //PG 요청 전 주문 상태 pending으로 변경
    @PostMapping("/ready")
    public ResponseEntity<?> readyPayment(@Valid @RequestBody PaymentReadyRequest request) {

        Payment payment = paymentService.ready(request.getOrderId(), request.getIdempotencyKey());

        return ResponseEntity.ok(new PaymentReadyResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getStatus()
        ));
    }


    //결제 인증 api 호출
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@Valid @RequestBody ConfirmPaymentRequest confirmPaymentRequest) {

        Payment payment =  paymentService.confirmPayment(
                confirmPaymentRequest.getAmount(),
                confirmPaymentRequest.getOrderId(),
                confirmPaymentRequest.getPaymentKey(),
                confirmPaymentRequest.getIdempotencyKey()
        );

        String message = switch (payment.getStatus()) {
            case SUCCESS -> "결제가 완료되었습니다.";
            case FAILED -> "결제에 실패했습니다.";
            case CONFIRMING -> "결제 결과를 확인 중입니다.";
            case READY -> "결제 승인이 시작되지 않았습니다.";
        };

        ConfirmPaymentResponse response = new ConfirmPaymentResponse(
                payment.getOrder().getId(),
                payment.getPaymentKey(),
                payment.getStatus().name(),
                message
        );

        //HTTP 202 반환, 승인 처리를 시작했지만 결과가 아직 확정되지 않았다는 의미
        if (payment.getStatus() == Payment.Status.CONFIRMING) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);
        }

        return ResponseEntity.ok(response);

    }



}