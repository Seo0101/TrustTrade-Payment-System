package org.example.trusttrade.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trusttrade.order.exception.ExternalApiException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.net.http.*;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TossPaymentClientTest {
    @Test void allRequestsHaveTimeoutAndApprovalAmountIsNumeric() throws Exception {
        var mapper = new ObjectMapper();
        var client = new TossPaymentClient(mapper);
        HttpClient original = (HttpClient) ReflectionTestUtils.getField(client, "httpClient");
        assertThat(original.connectTimeout()).contains(Duration.ofSeconds(10));
        HttpClient transport = mock(HttpClient.class);
        ReflectionTestUtils.setField(client, "httpClient", transport);
        ReflectionTestUtils.setField(client, "widgetSecretKey", "test_sk_fake_unit_test");
        client.requestConfirm(15000, "order", "pg-key");
        client.checkState("pg-key");
        client.requestPaymentCancel("pg-key", "cancel");
        var captured = ArgumentCaptor.forClass(HttpRequest.class);
        verify(transport, times(3)).send(captured.capture(), any(HttpResponse.BodyHandler.class));
        var requests = captured.getAllValues();
        assertThat(requests).allSatisfy(r -> assertThat(r.timeout()).contains(Duration.ofSeconds(30)));
        assertThat(requests.get(0).uri().getPath()).isEqualTo("/v1/payments/confirm");
        var result = new CompletableFuture<String>();
        var bytes = new java.io.ByteArrayOutputStream();
        requests.get(0).bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        var body = mapper.readTree(result.get(5, TimeUnit.SECONDS));
        assertThat(body.get("amount").isIntegralNumber()).isTrue();
        assertThat(body.get("amount").intValue()).isEqualTo(15000);
        assertThat(body.get("orderId").asText()).isEqualTo("order");
        assertThat(body.get("paymentKey").asText()).isEqualTo("pg-key");
    }
    @Test void interruptedApprovalRestoresThreadInterruptFlag() throws Exception {
        var client = new TossPaymentClient(new ObjectMapper());
        HttpClient transport = mock(HttpClient.class);
        ReflectionTestUtils.setField(client, "httpClient", transport);
        ReflectionTestUtils.setField(client, "widgetSecretKey", "test_sk_fake_unit_test");
        when(transport.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException());
        try {
            assertThatThrownBy(() -> client.requestConfirm(15000, "order", "pg-key"))
                    .isInstanceOf(ExternalApiException.class).hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
}
