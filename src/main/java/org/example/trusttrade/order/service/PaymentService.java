package org.example.trusttrade.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.domain.PaymentSyncResult;
import org.example.trusttrade.order.exception.*;
import org.example.trusttrade.order.exception.SystemException.DuplicatePaymentException;
import org.example.trusttrade.order.exception.SystemException.PaymentCreationFailedException;
import org.example.trusttrade.order.repository.OrderRepository;
import org.example.trusttrade.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.trusttrade.global.error.ErrorCode.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PaymentRepository paymentRepository;
    private final PaymentResultService paymentResultService;
    private final PaymentCreationService paymentCreationService;

    private final PaymentStateSyncService paymentStateSyncService;

    //=============위까지가 상태 조회, 결제 생성 관련 함수=================
    //싱태 조회
    public Payment getPaymentStatus(String orderId, String idempotencyKey) {


        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        //payment 검증
        if (!payment.getOrder().getId().equals(orderId)) {
            throw new BusinessException(INVALID_PAYMENT_ORDER);
        }

        // 이미 최종 상태라면 PG 조회하지 않음
        if (payment.getStatus() == Payment.Status.SUCCESS
                || payment.getStatus() == Payment.Status.FAILED) {
            return payment;
        }

        log.info("상태 조회 실행 중");

        // 3. PENDING, CONFIRMING일 때만 PG 조회
        if (order.getStatus() == Order.Status.PENDING &&
                payment.getStatus() == Payment.Status.CONFIRMING) {

            try {
                PaymentSyncResult result = paymentStateSyncService.checkAndSync(
                        payment.getPaymentKey(), payment.getIdempotencyKey());

            } catch (ExternalApiException e) {
                // PG 장애 시 상태 유지 + 사용자 안내 -> 스케줄러 전가
                log.error("PG 조회 통신 실패 orderId={}, key={}",
                        orderId,
                        idempotencyKey,
                        e
                );
            }
        }

        return payment;
    }


    //payment 검증
    private Payment validateExistingPayment(Payment payment, String orderId) {

        // 1. 멱등키가 다른 주문에 사용된 경우
        if (!payment.getOrder().getId().equals(orderId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PAYMENT_ORDER
            );
        }

        // 2. 이미 존재하는 결제이므로 그대로 반환
        return payment;
    }


    //결제 생성, createPayment()에만 트랜잭션
    public Payment ready(String orderId, String idempotencyKey) {

        //1.멱등키로 기존 payment 조회 및 반환
        //동일한 멱등키에 대한 중복 요청 방지
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return validateExistingPayment(
                    existing.get(),
                    orderId
            );
        }

        //2. 결제 신규 생성
        //createPayment()에 트랜잭션 설정되서 예외 발생하면 order 상태 변경, payment 생성이 모두 롤백됨.
        try {
            return paymentCreationService.createPayment(
                    orderId,
                    idempotencyKey
            );
        }
        //DuplicatePaymentException는 unique 충돌로 예외 잡아서 기존 payment 객체 반환
        //unique 충돌이 아닌 예외는 잡지 않고 글로벌로 전파해서 클라이언트한테 적절한 예외 응답
        catch (DuplicatePaymentException e) {
            Payment existingPayment =
                    paymentRepository
                            .findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() ->
                                    new PaymentCreationFailedException(
                                            "멱등키 충돌 후 Payment 조회 실패",
                                            e
                                    ));

            return validateExistingPayment(
                    existingPayment,
                    orderId);
        }

    }

    //===========================결제 인증 api 관련 함수==============================

    //결제 정보 인증 api 호출
    public Payment confirmPayment(int amount, String orderId, String paymentKey, String idempotencyKey) {

        // Transaction 1
        //결제 정보 검증
        paymentResultService.prepareConfirm(
                amount,
                orderId,
                paymentKey,
                idempotencyKey
        );

        // 결제 인증 api 호출
        HttpResponse<String> response = tossPaymentClient.requestConfirm(amount, orderId, paymentKey);

        // Transaction 2 -> 새로운 트랜잭션 시작되므로 기존 객체를 재사용 하는게 하니라 id로 다시 조회해서 사용해야 함.
        PaymentSyncResult result =
                paymentResultService.handleConfirmResult(
                        idempotencyKey,
                        response
                );

        // 이 시점에는 handleConfirmResult()의 TX2가 끝난 상태
        //NEEDS_SYNC는 상태 조회 및 동기화 필요하단 의미
        if (result == PaymentSyncResult.NEEDS_SYNC) {

            //외부 API + Transaction3
            paymentStateSyncService.checkAndSync(paymentKey, idempotencyKey);
        }

        return paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new BusinessException(PAYMENT_NOT_FOUND)
                );


    }


    //======================스케줄러 관련 함수==============
    //payment 상태 CONFIRMING 상태 결제 조회
    public List<Payment> syncConfirmingPayments(LocalDateTime threshold) {

        return paymentRepository.findByStatusWithLock(threshold);
    }



}