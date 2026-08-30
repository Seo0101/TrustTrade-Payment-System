package org.example.trusttrade.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PRODUCT_NOT_AVAILABLE("P001", HttpStatus.CONFLICT, "구매 불가능한 상품입니다."),
    NOT_RESERVED_BUYER("P002", HttpStatus.FORBIDDEN, "해당 구매자만 결제 가능합니다."),
    SELF_PURCHASE_NOT_ALLOWED("P003", HttpStatus.CONFLICT, "자기 상품 구매 불가"),
    PAYMENT_RETRY_LIMIT_EXCEEDED("P004", HttpStatus.TOO_MANY_REQUESTS, "결제 재시도 횟수 초과. 1분 뒤 다시 시도해주세요."),
    PAYABLE_NOT_POSSIBLE("P005", HttpStatus.CONFLICT, "재결제가 불가능한 주문입니다. 1분 뒤 주문 상태를 다시 확인해주세요."),
    PAYMENT_ALREADY_PROCESSED("P006", HttpStatus.CONFLICT, "결제가 완료된 주문입니다. 주문 상태를 다시 확인해주세요."),
    INVALID_PAYMENT_ORDER("P007", HttpStatus.BAD_REQUEST, "주문 정보와 일치하지 않은 결제입니다."),
    PAYMENT_IN_PROGRESS("P008", HttpStatus.CONFLICT, "진행 중인 주문입니다."),
    ALREADY_PURCHASED("P009", HttpStatus.CONFLICT, "이미 구매한 상품입니다."),
    ORDER_NOT_FOUND("P010", HttpStatus.NOT_FOUND, "주문 정보가 없습니다."),
    NOT_FOUND_PAYMENT_SESSION("P11", HttpStatus.NOT_FOUND, "결제 세션을 찾을 수 없습니다."),
    PAYMENT_REJECTED("P12", HttpStatus.BAD_REQUEST, "결제가 승인되지 않았습니다. 결제수단을 확인해주세요."),
    PAYMENT_PROVIDER_ERROR("P13", HttpStatus.BAD_GATEWAY, "결제 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_PAYMENT_REQUEST("P14", HttpStatus.BAD_REQUEST, "잘못된 조회 요청입니다."),
    PAYMENT_QUERY_LIMIT_EXCEEDED("P15", HttpStatus.TOO_MANY_REQUESTS, "너무 많은 조회 요청으로 실패하였습니다."),
    PAYMENT_INTEGRATION_ERROR("P16", HttpStatus.BAD_GATEWAY, "서버에 일시적인 문제가 발생했습니다. 잠시 후에 다시 시도해주세요."),
    PAYMENT_UNKNOWN_ERROR("P17",HttpStatus.INTERNAL_SERVER_ERROR, "결제 처리 중 알 수 없는 오류가 발생했습니다."),
    PAYMENT_NOT_FOUND("P18",HttpStatus.NOT_FOUND, "결제 정보가 없습니다." ),
    ORDER_NOT_PENDING("P19", HttpStatus.CONFLICT, "주문의 상태가 PENDING 상태가 아닙니다."),
    PAYMENT_NOT_CONFIRMING("P20", HttpStatus.CONFLICT, "결제의 상태가 CONFIRMING 상태가 아닙니다."),
    PRODUCT_NOT_FOUND("P21", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_RESERVED("P22", HttpStatus.CONFLICT, "상품이 상태가 RESERVED 상태가 아닙니다."),
    USER_NOT_FOUND("P23", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");




    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getStatus() { return status; }
}
