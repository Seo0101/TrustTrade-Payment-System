package org.example.trusttrade.order.util;

import java.sql.SQLException;

public class DatabaseExceptionUtils {

    //static으로만 사용할 클래스이므로 생성자 접근 불가능 하도록 private 설정
    private DatabaseExceptionUtils() {}

    //DB레벨 중복 주문 생성 방지
    //static 메서드 > 의존성 주입 X > 빈 등록 X
    public static boolean isDuplicateKeyException(Throwable e) {
        Throwable cause = e;

        while (cause != null) {

            // Spring DataAccessException 계열 체크
            if (cause instanceof org.springframework.dao.DuplicateKeyException) {
                return true;
            }

            if (cause instanceof SQLException sqlException) {
                for (SQLException sql = sqlException;
                     sql != null;
                     sql = sql.getNextException()) {

                    //JDBC 예외에 연결된 추가 SQL 예외
                    if (sql.getErrorCode() == 1062) {
                        return true;
                    }
                }
            }

            cause = cause.getCause();
        }

        //이외 예외는 중복 예외 말고 처리
        return false;
    }
}
