package org.example.trusttrade.order.util;

public class DatabaseExceptionUtils {

    private DatabaseExceptionUtils() {}

    //DB레벨 중복 주문 생성 방지
    public static boolean isDuplicateKeyException(Throwable e) {
        Throwable cause = e;

        while (cause != null) {

            // 1. MySQL / PostgreSQL / Oracle 공통 체크
            if (cause instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return true;
            }

            // 2. Spring DataAccessException 계열 체크
            if (cause instanceof org.springframework.dao.DuplicateKeyException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
