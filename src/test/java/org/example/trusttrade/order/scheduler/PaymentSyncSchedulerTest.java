package org.example.trusttrade.order.scheduler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.domain.PaymentSyncResult;
import org.example.trusttrade.order.exception.ExternalApiException;
import org.example.trusttrade.order.service.PaymentService;
import org.example.trusttrade.order.service.PaymentStateSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentSyncSchedulerTest {
    private final PaymentService payments = mock(PaymentService.class);
    private final PaymentStateSyncService sync = mock(PaymentStateSyncService.class);
    private final PaymentSyncScheduler scheduler = new PaymentSyncScheduler(payments, sync);

    private Payment target(long id, String key) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(id);
        when(payment.getPaymentKey()).thenReturn("pg-" + key);
        when(payment.getIdempotencyKey()).thenReturn(key);
        return payment;
    }

    @Test void emptyTargetsDoNotCallPgAndThresholdIsFiveMinutesAgo() {
        when(payments.syncConfirmingPayments(any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);
        scheduler.syncPayments();
        var threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(payments).syncConfirmingPayments(threshold.capture());
        assertThat(threshold.getValue()).isBetween(before, LocalDateTime.now().minusMinutes(5));
        verifyNoInteractions(sync);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void oneFailureDoesNotPreventProcessingNextTarget(boolean communicationFailure) {
        doReturn(List.of(target(1, "first"), target(2, "second")))
                .when(payments).syncConfirmingPayments(any());
        RuntimeException error = communicationFailure
                ? new ExternalApiException("timeout", new java.io.IOException())
                : new IllegalStateException("state mismatch");
        when(sync.checkAndSync("pg-first", "first")).thenThrow(error);
        when(sync.checkAndSync("pg-second", "second")).thenReturn(PaymentSyncResult.SUCCESS);
        assertThatCode(scheduler::syncPayments).doesNotThrowAnyException();
        verify(sync).checkAndSync("pg-second", "second");
    }

    @Test void reconciliationRequiredProducesErrorLogAndContinuesToNextTarget() {
        doReturn(List.of(target(1, "first"), target(2, "second")))
                .when(payments).syncConfirmingPayments(any());
        when(sync.checkAndSync("pg-first", "first")).thenReturn(PaymentSyncResult.NEEDS_SYNC);
        when(sync.checkAndSync("pg-second", "second")).thenReturn(PaymentSyncResult.PENDING);
        Logger logger = (Logger) LoggerFactory.getLogger(PaymentSyncScheduler.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start(); logger.addAppender(logs);
        try {
            scheduler.syncPayments();
            assertThat(logs.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage()).contains("정합성 확인 필요", "paymentId=1");
            });
            verify(sync).checkAndSync("pg-second", "second");
        } finally { logger.detachAppender(logs); logs.stop(); }
    }
}
