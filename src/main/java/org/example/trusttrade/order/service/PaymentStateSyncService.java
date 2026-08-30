package org.example.trusttrade.order.service;


import lombok.RequiredArgsConstructor;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.domain.PaymentSyncResult;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class PaymentStateSyncService {

    private final TossPaymentClient tossPaymentClient;
    private final PaymentResultService paymentResultService;

    public PaymentSyncResult checkAndSync(String paymentKey, String idempotencyKey) {

        //외부 API
        HttpResponse<String> response =
                tossPaymentClient.checkState(paymentKey);

        // Transaction 3 - 결제 조회 후 상태 처리
       return paymentResultService.syncPaymentState(
               idempotencyKey,
                response
        );
    }
}
