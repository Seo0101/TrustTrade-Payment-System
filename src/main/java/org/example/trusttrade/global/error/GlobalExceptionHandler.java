package org.example.trusttrade.global.error;

import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.order.exception.*;
import org.example.trusttrade.order.exception.SystemException.OrderCreationFailedException;
import org.example.trusttrade.order.exception.SystemException.PaymentCreationFailedException;
import org.example.trusttrade.order.exception.SystemException.PaymentSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotAllowUserType.class)
    public ResponseEntity<Map<String, Object>> handleNotAllowUserType(NotAllowUserType e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", e.getMessage()
                ));
    }


    //주문 취소 성공 but 내부 상태 변경 실패
    @ExceptionHandler(OrderStateUpdateException.class)
    public ResponseEntity<?> handleOrderStateUpdate(OrderStateUpdateException e) {
        log.error("주문 상태 변경 예외", e);
        return ResponseEntity.status(400).body(e.getMessage());
    }

    //결제 취소 api 응답 실패 or 네트워크 실패
    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<String> handleOrderCancellationException(OrderCancellationException e) {
        return ResponseEntity
                .status(500)
                .body(e.getErrorCode() + " : " + e.getMessage());
    }

    //주문 정보 불일치
    @ExceptionHandler(PaymentAmountMismatchException.class)
    public ResponseEntity<String> handlePaymentAmountMismatch(PaymentAmountMismatchException e) {
        return ResponseEntity
                .status(500)
                .body(e.getMessage());
    }

    //주문 정보 없음
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<String> handleOrderNotFound(OrderNotFoundException e) {
        return ResponseEntity
                .status(500)
                .body(e.getMessage());
    }

    //상품 정보 없음 or 사용자 정보 없음
    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {

        ErrorCode errorCode = e.getErrorCode();

        log.error(
                "비즈니스 예외 발생. code={}, message={}",
                errorCode.getCode(),
                errorCode.getMessage(),
                e
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(Map.of(
                        "code", errorCode.getCode(),
                        "message", errorCode.getMessage()
                ));
    }

    //예상치 못한 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {

        log.error("예상하지 못한 예외", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_SERVER_ERROR",
                        "message", "서버 오류가 발생했습니다."
                ));
    }

    //외부 API 에러
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<?> handleExternalApiException(ExternalApiException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", e.getCause(),
                        "message", e.getMessage()
                ));
    }

    //결제 생성 중 서버 예외 발생
    //전달 받은 e는 클라이언트한테 그대로 전달하지 않도록 해야함.
    @ExceptionHandler(PaymentCreationFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentCreationFailed(
            PaymentCreationFailedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "PAYMENT_CREATION_FAILED",
                        "결제 생성에 실패했습니다."
                ));
    }

    //주문 생성 중 서버 예외 발생
    @ExceptionHandler(OrderCreationFailedException.class)
    public ResponseEntity<ErrorResponse> handleOrderCreationFailed(
            OrderCreationFailedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "ORDER_CREATION_FAILED",
                        "주문 생성에 실패했습니다."
                ));
    }

    //주문 조회 시 주문 상태 이상(주문 멱등키 존재 + cancelled 상태)
    @ExceptionHandler(OrderStateException.class)
    public ResponseEntity<ErrorResponse> handleOrderStateException(
            OrderStateException e
    ) {
        //서버 내부 예외는 글로벌에서 로그 남겨서 일괄적으로 관리
        log.error("주문 상태 정합성 오류", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "ORDER_STATE_ERROR",
                        "주문 처리 중 서버 오류가 발생했습니다."
                ));
    }

    //결제 승인 결과 처리 중 PG사, 서버 장애
        @ExceptionHandler(PaymentSystemException.class)
        public ResponseEntity<ErrorResponse> handlePaymentSystemException(
                PaymentSystemException e
        ) {

            ErrorCode errorCode = e.getErrorCode();

            ErrorResponse response = new ErrorResponse(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );

            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(response);
        }

    //



}
