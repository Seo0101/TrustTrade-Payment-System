package org.example.trusttrade.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.domain.PaymentSyncResult;
import org.example.trusttrade.order.dto.ConfirmPreparation;
import org.example.trusttrade.order.exception.*;
import org.example.trusttrade.order.exception.SystemException.PaymentSystemException;
import org.example.trusttrade.order.repository.OrderRepository;
import org.example.trusttrade.order.repository.PaymentRepository;
import org.example.trusttrade.order.util.PaymentResponseParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpResponse;

import static org.example.trusttrade.global.error.ErrorCode.*;
import static org.example.trusttrade.order.domain.Order.Status.PENDING;

@Service
@Slf4j
@AllArgsConstructor
//PG 응답 해석 + DB 상태 변경
public class PaymentResultService {

    private final PaymentResponseParser paymentResponseParser;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // 반드시 트랜잭션 안에서 호출
    private Payment findPaymentWithOrderLock(String idempotencyKey) {
        // ID만 조회: 이 단계에서 상태를 판단하지 않음
        String orderId = paymentRepository
                .findOrderIdByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 1. 주문 락
        orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 결제 락 + 최신 결제 조회
        return paymentRepository
                .findByIdempotencyKeyForUpdate(idempotencyKey)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    //===============인증 api 호출 전 처리 트랜잭션=============
    @Transactional
    public ConfirmPreparation prepareConfirm(int amount, String orderId, String paymentKey, String idempotencyKey) {

        Payment payment = findPaymentWithOrderLock(idempotencyKey);

        // 상태와 관계없이 주문 ID와 금액부터 검증
        Order order = verifyPayment(payment, orderId, amount);

        if (payment.getStatus() != Payment.Status.READY) {
            // 반복 요청은 기존에 저장된 paymentKey와도 일치해야 함
            if (!paymentKey.equals(payment.getPaymentKey())) {
                throw new BusinessException(
                        ErrorCode.INVALID_PAYMENT_REQUEST
                );
            }

            // CONFIRMING / SUCCESS / FAILED는 PG 승인 재호출 안 함
            return new ConfirmPreparation(
                    false,
                    order.getId(),
                    order.getAmount(),
                    payment.getPaymentKey(),
                    payment.getIdempotencyKey()
            );
        }

        if (order.getOrderLockKey() == null
                || order.getStatus() != Order.Status.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }

        payment.assignPaymentKey(paymentKey);
        payment.changeStatus(Payment.Status.CONFIRMING);

        return new ConfirmPreparation(
                true,
                order.getId(),
                order.getAmount(),
                payment.getPaymentKey(),
                payment.getIdempotencyKey()
        );

    }

    //결제 정보 검증
    public Order verifyPayment(Payment payment, String orderId, int amount) {

        Order order = payment.getOrder();

        if (!order.getId().equals(orderId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PAYMENT_ORDER
            );
        }

        if (order.getAmount() != amount) {
            throw new PaymentAmountMismatchException(order);
        }
        return order;
    }


    //=================결과 처리 트랜잭션 함수==================
    private void verifySuccessfulPgResponse(
            Payment payment,
            HttpResponse<String> response
    ) {
        if (response.statusCode() != 200) {
            return;
        }

        JsonNode root = paymentResponseParser.parse(response.body());

        if (!"DONE".equals(root.path("status").asText())) {
            return;
        }

        Order order = payment.getOrder();
        JsonNode totalAmount = root.get("totalAmount");

        boolean matches =
                order.getId().equals(root.path("orderId").asText())
                        && payment.getPaymentKey() != null
                        && payment.getPaymentKey().equals(
                        root.path("paymentKey").asText()
                )
                        && totalAmount != null
                        && totalAmount.isIntegralNumber()
                        && totalAmount.canConvertToInt()
                        && totalAmount.intValue() == order.getAmount()
                        && totalAmount.intValue() == payment.getAmount();

        if (!matches) {
            log.error(
                    "PG 성공 응답 불일치 orderId={}, paymentId={}",
                    order.getId(),
                    payment.getId()
            );

            throw new PaymentSystemException(
                    ErrorCode.PAYMENT_INTEGRATION_ERROR
            );
        }
    }

    @Transactional
    public PaymentSyncResult handleConfirmResult(
            String idempotencyKey,
            HttpResponse<String> response
    ) {

        // 주문 → 결제 순서로 락 획득
        //외부 API 호출 끝남 -> idempotencyKey로 다시 payment 조회
        Payment payment = findPaymentWithOrderLock(idempotencyKey);

        verifySuccessfulPgResponse(payment, response);

        //이미 확정된 결제인지 확인
        PaymentSyncResult finalResult = existingFinalResult(payment, response);

        if (finalResult != null) {
            return finalResult;
        }

        //미확정 결제는 CONFIRMING인지 검증
        if (payment.getStatus() != Payment.Status.CONFIRMING) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_NOT_CONFIRMING
            );
        }

        //승인 결과 처리 및 상태 반환
        return handlePaymentResult(
                payment.getOrder(),
                payment,
                response
        );
    }

    //결제 승인 결과 처리 -> handlePaymentFailure() 호출한 후에 예외 발생하면, payment 상태 변경이 모두 롤백됨.-> 스케러로 처리됨
    public PaymentSyncResult handlePaymentResult(Order order, Payment payment, HttpResponse<String> response) {
        int statusCode = response.statusCode();

        if (statusCode != 200) {

            String errorCode = paymentResponseParser.extractErrorCode(response.body());
            String errorMessage = paymentResponseParser.extractErrorMessage(response.body());


            log.error("PG 응답 실패 code={}, orderId={}, errorCode={}, errorMessage={}",
                    statusCode, order.getId(), errorCode, errorMessage);

            switch (errorCode) {
                //이미 처리된 결제 > 상태 조회 후 동기화==============================
                case "ALREADY_PROCESSED_PAYMENT":
                    //여기서 바로 상태 동기화 함수 호출하면 트랜잭션(t2) 내에서 외부 API 호출하므로 안됨
                    //상태만 반환해서 이후 동기화 처리
                    return PaymentSyncResult.NEEDS_SYNC;

                    //==========실패 확정===============
                    //결제 세션 만료로 더이상 승인 불가 -> 실패 확정
                case "NOT_FOUND_PAYMENT_SESSION":
                    //카드/계좌 거절 -> 실패 확정
                case "INVALID_CARD_NUMBER":
                case "INVALID_CARD_LOST_OR_STOLEN":
                case "REJECT_CARD_PAYMENT":
                case "REJECT_CARD_COMPANY":
                case "REJECT_ACCOUNT_PAYMENT":
                    handlePaymentFailure(order, payment, errorMessage);
                    return PaymentSyncResult.FAILED;

                //==========실패 확정 x============
                //PG 장애 -> 상태 확정 X, 스케줄러 전가(조회 후 확정)
                case "PROVIDER_ERROR":
                case "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING":
                case "FAILED_CARD_COMPANY_RESPONSE":
                    throw new PaymentSystemException(PAYMENT_PROVIDER_ERROR);

                    //인증 오류(서버 설정 문제) -> 스케줄러 전가(조회 및 검증 후 확정)
                case "UNAUTHORIZED_KEY":
                case "INVALID_AUTHORIZE_AUTH":
                case "FORBIDDEN_REQUEST":
                case "INVALID_REQUEST":
                    throw new PaymentSystemException(PAYMENT_INTEGRATION_ERROR);

                default:
                    log.error("Unknown Toss errorCode={}, orderId={}", errorCode, order.getId());
                    throw new PaymentSystemException(PAYMENT_UNKNOWN_ERROR);
            }
        }

        //성공 응답 처리
        String status = paymentResponseParser.extractStatus(response.body());

        if ("DONE".equals(status)) {
            handlePaymentSuccess(order, payment);
            return PaymentSyncResult.SUCCESS;
        }

        // 200이지만 아직 성공 확정 상태가 아님
        log.warn(
                "PG 승인 응답 200이지만 상태 미확정 status={}, orderId={}",
                status,
                order.getId()
        );

        return PaymentSyncResult.PENDING;

    }

    //결제성공 -> 만약 handlePaymentResult()에서 호출된거라면 기존 트랜잭션에 합류함
    @Transactional
    public void handlePaymentSuccess(Order order, Payment payment) {

        order.paidOrder();
        order.getProduct().setCompleted();
        payment.changeStatus(Payment.Status.SUCCESS);

        log.info("결제 성공 orderId={}", order.getId());
    }

    //결제 실패
    @Transactional
    public void handlePaymentFailure(Order order, Payment payment, String failureReason) {

        //active = true, product = reserve 상태 유지 -> 계속 유지시 스케줄러 처리
        order.increaseRetryCount();

        if (order.getRetryCount() >= 3) {
            order.cancelOrder(); //재시도 불가 (상태 cancel, lockKey = null)
            order.getProduct().resale(); //주문의 상품 정보(상태, 예약자) 리셋
            payment.changeStatus(Payment.Status.FAILED);
            payment.setFailReason(failureReason);
        } else {
            order.failOrder(); //재시도 가능
            payment.changeStatus(Payment.Status.FAILED);
            payment.setFailReason(failureReason);
        }

        log.info("결제 실패 orderId={}", order.getId());
    }

    //==============상태 동기화====================
    //상태 조회 결과 처리
    @Transactional
    public PaymentSyncResult syncPaymentState(
            String idempotencyKey,
            HttpResponse<String> response
    ) {

        //외부 api 호출 후 다시 조회
        Payment payment = findPaymentWithOrderLock(idempotencyKey);

        verifySuccessfulPgResponse(payment, response);

        PaymentSyncResult finalResult = existingFinalResult(payment, response);

        if (finalResult != null) {
            return finalResult;
        }

        Order order = payment.getOrder();

        //응답 상태 추출
        int statusCode = response.statusCode();

        // ==========================
        // 1. PG 조회 성공
        // ==========================
        if (statusCode == 200) {

            String status = paymentResponseParser.extractStatus(response.body());

            if ("DONE".equals(status)) {

                log.info(
                        "PG 상태 동기화 성공 orderId={}, paymentKey={}",
                        order.getId(),
                        payment.getPaymentKey()
                );

                handlePaymentSuccess(order, payment);

                return PaymentSyncResult.SUCCESS;
            }

            // 200이지만 아직 확정할 수 없는 상태
            log.warn(
                    "PG 조회 결과 미확정 status={}, orderId={}, paymentKey={}",
                    status,
                    order.getId(),
                    payment.getPaymentKey()
            );

            return PaymentSyncResult.PENDING;

        } else {

            // ==========================
            // 2. PG 오류 응답
            // ==========================

            // 조회 404만으로 승인 실패를 확정하지 않는다.
            // 오류 응답 파싱에 실패해도 상태를 바꾸지 않도록 먼저 처리한다.
            if (statusCode == 404) {
                log.warn("PG 결제 조회 미확정 orderId={}, paymentKey={}",
                        order.getId(), payment.getPaymentKey());
                return PaymentSyncResult.PENDING;
            }

            String errorCode = paymentResponseParser.extractErrorCode(response.body());
            String errorMessage = paymentResponseParser.extractErrorMessage(response.body());

            // 3. 인증/권한 오류
            if (statusCode == 401 || statusCode == 403) {

                log.error(
                        "PG 상태 조회 인증/권한 오류 errorCode={}, message={}, orderId={}, paymentKey={}",
                        errorCode,
                        errorMessage,
                        order.getId(),
                        payment.getPaymentKey()
                );

                //confirming 유지
                return PaymentSyncResult.PENDING;
            }

            // 4. PG 서버 오류
            if (statusCode >= 500) {

                log.error(
                        "PG 상태 조회 서버 오류 errorCode={}, message={}, orderId={}, paymentKey={}",
                        errorCode,
                        errorMessage,
                        order.getId(),
                        payment.getPaymentKey()
                );

                //confirming 유지
                return PaymentSyncResult.PENDING;
            }

            // 5. 그 외 예상하지 못한 응답
            log.warn(
                    "PG 상태 조회 미처리 응답 statusCode={}, errorCode={}, message={}, orderId={}, paymentKey={}",
                    statusCode,
                    errorCode,
                    errorMessage,
                    order.getId(),
                    payment.getPaymentKey()
            );
            return PaymentSyncResult.PENDING;
            //confirming 유지
            // → 다음 스케줄러에서 재조회
        }


    }
    // 확정 상태는 중복 반영하지 않음. 실패 기록과 성공 응답의 충돌은 재조회한다.
    private PaymentSyncResult existingFinalResult(Payment payment, HttpResponse<String> response) {
        if (payment.getStatus() == Payment.Status.SUCCESS) {
            return PaymentSyncResult.SUCCESS;
        }
        if (payment.getStatus() == Payment.Status.FAILED) {
            if (response.statusCode() == 200
                    && "DONE".equals(paymentResponseParser.extractStatus(response.body()))) {
                log.error("결제 상태 불일치: DB=FAILED, PG=DONE, orderId={}, paymentKey={}",
                        payment.getOrder().getId(), payment.getPaymentKey());
                return PaymentSyncResult.NEEDS_SYNC;
            }
            return PaymentSyncResult.FAILED;
        }
        return null;
    }

}
