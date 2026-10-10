package org.example.trusttrade.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.domain.PaymentSyncResult;
import org.example.trusttrade.order.exception.ExternalApiException;
import org.example.trusttrade.order.service.PaymentService;
import org.example.trusttrade.order.service.PaymentStateSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentSyncScheduler {

    private final PaymentService paymentService;
    private final PaymentStateSyncService paymentStateSyncService;


    //PG 상태 동기화 스케줄러(order-detail.html에서 PG조회 실패한 주문 대응)
    //이전 실행의 종료로부터 60초 이후 실행
    @Scheduled(fixedDelay = 60000)
    public void syncPayments() {

        //5분 이상 confirming 상태 결제 객체 조회
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        //confirming 상태 조회
        List<Payment> targets = paymentService.syncConfirmingPayments(threshold);

        for (Payment payment : targets) {
            try {

                //결제 상태 조회 + Transaction1
                PaymentSyncResult result =
                        paymentStateSyncService.checkAndSync(payment.getPaymentKey(), payment.getIdempotencyKey());


                if (result == PaymentSyncResult.NEEDS_SYNC) {
                    log.error(
                            "결제 상태 정합성 확인 필요 paymentId={}",
                            payment.getId()
                    );
                    // 모니터링 알림 연동
                } else if (result == PaymentSyncResult.PENDING) {
                    log.debug(
                            "결제 결과 미확정 paymentId={}",
                            payment.getId()
                    );
                }

                // PG 장애 시 상태 유지 + 사용자 안내 -> 스케줄러 전가
            } catch (ExternalApiException e) {
                log.error("PG 조회 통신실패 paymentId={}", payment.getId(), e);

                //예상하지 못한 예외 발생 -> 모니터링 시스템에 알림
            } catch (Exception e) {
                log.error("PG 조회 실패 paymentId={}", payment.getId(), e);

                //모니터링 시스템 알림 로직 추가
            }

        }

    }
}

