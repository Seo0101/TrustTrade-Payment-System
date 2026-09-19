package org.example.trusttrade.order.util;

public class DatabaseExceptionUtils {

    //static으로만 사용할 클래스이므로 생성자 접근 불가능 하도록 private 설정
    private DatabaseExceptionUtils() {}

    //DB레벨 중복 주문 생성 방지
    //static 메서드 > 의존성 주입 X > 빈 등록 X
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
