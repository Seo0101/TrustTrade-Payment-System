package org.example.trusttrade.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.dto.*;
import org.example.trusttrade.order.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    //PG 요청 전 주문 상태 pending으로 변경
    @PostMapping("/ready")
    public ResponseEntity<?> readyPayment(@RequestBody PaymentReadyRequest request) {

        Payment payment = paymentService.ready(request.getOrderId(), request.getIdempotencyKey());

        return ResponseEntity.ok(new PaymentReadyResponse(
                payment.getId(),
                payment.getIdempotencyKey()
        ));
    }


    //결제 인증 api 호출
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmPaymentRequest confirmPaymentRequest) {

        Payment payment =  paymentService.confirmPayment(
                confirmPaymentRequest.getAmount(),
                confirmPaymentRequest.getOrderId(),
                confirmPaymentRequest.getPaymentKey(),
                confirmPaymentRequest.getIdempotencyKey()
        );

        return ResponseEntity.ok(new ConfirmPaymentResponse(
                payment.getOrder().getId(),
                payment.getPaymentKey(),
                payment.getStatus().name(),
                "결제 인증 처리 완료"
        ));

    }



}