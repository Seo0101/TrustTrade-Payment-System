package org.example.trusttrade.auction.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.service.AuctionOrderService;
import org.example.trusttrade.auction.service.AuctionPaymentService;
import org.example.trusttrade.auction.service.DepositOrderService;
import org.example.trusttrade.order.dto.ConfirmPaymentRequest;
import org.example.trusttrade.order.dto.PaymentErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auctionPayments")
@RequiredArgsConstructor
public class AuctionPaymentController {

    @Autowired
    private final AuctionPaymentService auctionPaymentService;

    //결제 인증 api 호출
    @PostMapping("/confirm")
    public ResponseEntity confirm(@RequestBody ConfirmPaymentRequest confirmPaymentRequest) {
        try {
            boolean success = auctionPaymentService.confirmAndSavePayment(confirmPaymentRequest);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "결제 승인 및 저장 성공"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "DB 반영 실패"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    PaymentErrorResponse.builder()
                            .code(400)
                            .message("결제 승인 중 에러 발생")
                            .build()
            );
        }
    }
}