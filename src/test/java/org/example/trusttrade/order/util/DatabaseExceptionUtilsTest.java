package org.example.trusttrade.order.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseExceptionUtilsTest {
    @Test void recognizesWrappedMysqlDuplicateKey() {
        SQLException sql = new SQLIntegrityConstraintViolationException("duplicate", "23000", 1062);
        assertThat(DatabaseExceptionUtils.isDuplicateKeyException(
                new DataIntegrityViolationException("save failed", new RuntimeException(sql)))).isTrue();
    }
    @Test void recognizesSpringDuplicateKey() {
        assertThat(DatabaseExceptionUtils.isDuplicateKeyException(
                new RuntimeException(new DuplicateKeyException("duplicate")))).isTrue();
    }
    @Test void recognizesDuplicateInAdditionalSqlException() {
        SQLException sql = new SQLException("batch failure");
        sql.setNextException(new SQLException("duplicate", "23000", 1062));
        assertThat(DatabaseExceptionUtils.isDuplicateKeyException(sql)).isTrue();
    }
    @Test void doesNotTreatForeignKeyOrNotNullViolationsAsDuplicates() {
        for (int code : new int[]{1452, 1048}) {
            assertThat(DatabaseExceptionUtils.isDuplicateKeyException(new DataIntegrityViolationException(
                    "integrity failure", new SQLIntegrityConstraintViolationException("invalid", "23000", code))))
                    .isFalse();
        }
    }
    @Test void unrelatedExceptionIsNotDuplicate() {
        assertThat(DatabaseExceptionUtils.isDuplicateKeyException(new IllegalArgumentException("invalid"))).isFalse();
    }
}
