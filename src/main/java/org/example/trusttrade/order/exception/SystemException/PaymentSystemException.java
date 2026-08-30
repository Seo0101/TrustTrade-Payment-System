package org.example.trusttrade.order.exception.SystemException;

import org.example.trusttrade.global.error.ErrorCode;

public class PaymentSystemException extends RuntimeException {

    private final ErrorCode errorCode;

    public PaymentSystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
