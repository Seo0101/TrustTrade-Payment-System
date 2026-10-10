package org.example.trusttrade.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.example.trusttrade.global.error.GlobalExceptionHandler;
import org.example.trusttrade.item.domain.products.*;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.controller.PaymentController;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP requests through real ready services, transaction proxies and isolated H2 persistence. */
class PaymentConfirmApiIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private EntityManager em;
    private TransactionTemplate tx;
    private MockMvc mvc;
    private PaymentRepository payments;
    private String orderId;
    private final ObjectMapper json = new ObjectMapper();

    @Configuration
    @EnableTransactionManagement
    @Import({PaymentCreationService.class, PaymentService.class, PaymentResultService.class,
            PaymentStateSyncService.class, org.example.trusttrade.order.util.PaymentResponseParser.class})
    static class Config {
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(new DriverManagerDataSource(
                    "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", ""));
            factory.setPackagesToScan("org.example.trusttrade");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
        @Bean EntityManager entityManager(EntityManagerFactory emf) {
            return SharedEntityManagerCreator.createSharedEntityManager(emf);
        }
        @Bean OrderRepository orders(EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(OrderRepository.class);
        }
        @Bean PaymentRepository payments(EntityManager em) {
            return mock(PaymentRepository.class, org.mockito.AdditionalAnswers.delegatesTo(
                    new JpaRepositoryFactory(em).getRepository(PaymentRepository.class)));
        }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean TossPaymentClient toss() { return mock(TossPaymentClient.class); }
    }

    @BeforeEach void start() {
        context = new AnnotationConfigApplicationContext(Config.class);
        em = context.getBean(EntityManager.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        payments = context.getBean(PaymentRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(context.getBean(PaymentService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        orderId = seedOrder();
        context.getBean(PaymentCreationService.class).createPayment(orderId, "key");
    }
    @AfterEach void stop() { if (context != null) context.close(); }

    private String seedOrder() {
        return tx.execute(s -> {
            User buyer = user();
            User seller = user();
            Product product = Product.builder().name("상품").description("설명").user(seller)
                    .productPrice(15000).status(ProductStatus.SALE).createdTime(LocalDateTime.now()).build();
            product.reserve(buyer);
            em.persist(product);
            Order order = Order.create(product, buyer, seller, UUID.randomUUID().toString());
            em.persist(order);
            em.flush();
            return order.getId();
        });
    }
    private User user() {
        User user = User.builder().role(User.Role.USER).memberType(User.MemberType.GENERAL)
                .roughAddress("서울").build();
        em.persist(user);
        return user;
    }
    private ResultActions request(String order, String key) throws Exception {
        return mvc.perform(post("/payments/ready").contentType("application/json")
                .content(json.writeValueAsString(Map.of("orderId", order, "idempotencyKey", key))));
    }
    private long count() { return tx.execute(s -> payments.count()); }
    private Order.Status orderStatus() { return tx.execute(s -> em.find(Order.class, orderId).getStatus()); }
    private long responseId(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("paymentId").asLong();
    }


    private TossPaymentClient client() { return context.getBean(TossPaymentClient.class); }
    private ResultActions confirm(int amount, String order, String pgKey) throws Exception {
        return mvc.perform(post("/payments/confirm").contentType("application/json")
                .content(json.writeValueAsString(Map.of("amount", amount, "orderId", order,
                        "paymentKey", pgKey, "idempotencyKey", "key"))));
    }
    @SuppressWarnings("unchecked")
    private java.net.http.HttpResponse<String> pg(int status, String body) {
        java.net.http.HttpResponse<String> response = mock(java.net.http.HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
    private String done() throws Exception {
        return json.writeValueAsString(Map.of("status", "DONE", "orderId", orderId,
                "paymentKey", "pg-key", "totalAmount", 15000));
    }
    private void assertState(Payment.Status payment, Order.Status order, ProductStatus product, int retries) {
        tx.executeWithoutResult(s -> {
            Payment stored = payments.findByIdempotencyKey("key").orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(payment);
            assertThat(stored.getOrder().getStatus()).isEqualTo(order);
            assertThat(stored.getOrder().getProduct().getStatus()).isEqualTo(product);
            assertThat(stored.getOrder().getRetryCount()).isEqualTo(retries);
        });
    }
    @Test void successCommitsPreparationBeforeCallingPgAndRepeatedRequestDoesNotApproveAgain() throws Exception {
        var response = pg(200, done());
        when(client().requestConfirm(15000, orderId, "pg-key")).thenAnswer(i -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()).isFalse();
            assertState(Payment.Status.CONFIRMING, Order.Status.PENDING, ProductStatus.RESERVED, 0);
            return response;
        });
        confirm(15000, orderId, "pg-key").andExpect(status().isOk())
                .andExpect(jsonPath("status").value("SUCCESS"))
                .andExpect(jsonPath("paymentKey").value("pg-key"));
        confirm(15000, orderId, "pg-key").andExpect(status().isOk()).andExpect(jsonPath("status").value("SUCCESS"));
        verify(client(), times(1)).requestConfirm(15000, orderId, "pg-key");
        assertState(Payment.Status.SUCCESS, Order.Status.PAID, ProductStatus.COMPLETED, 0);
    }
    @Test void rejectionFailsOnlyOnceAndKeepsReservation() throws Exception {
        doReturn(pg(400, "{\"code\":\"REJECT_CARD_PAYMENT\",\"message\":\"rejected\"}")).when(client()).requestConfirm(anyInt(), anyString(), anyString());
        for (int i=0; i<2; i++) confirm(15000, orderId, "pg-key").andExpect(status().isOk())
                .andExpect(jsonPath("status").value("FAILED"));
        verify(client(), times(1)).requestConfirm(anyInt(), anyString(), anyString());
        assertState(Payment.Status.FAILED, Order.Status.FAILED, ProductStatus.RESERVED, 1);
    }
    @Test void thirdRejectionCancelsOrderAndReleasesReservationAndOrderKey() throws Exception {
        tx.executeWithoutResult(s -> {
            Order order = em.find(Order.class, orderId);
            order.increaseRetryCount(); order.increaseRetryCount();
        });
        doReturn(pg(400, "{\"code\":\"REJECT_CARD_PAYMENT\",\"message\":\"rejected\"}")).when(client()).requestConfirm(anyInt(), anyString(), anyString());
        confirm(15000, orderId, "pg-key").andExpect(status().isOk());
        assertState(Payment.Status.FAILED, Order.Status.CANCELLED, ProductStatus.SALE, 3);
        tx.executeWithoutResult(s -> {
            Order order = em.find(Order.class, orderId);
            assertThat(order.getOrderLockKey()).isNull();
            assertThat(order.getProduct().getReservedBuyer()).isNull();
        });
    }
    @Test void communicationFailureReturns502AndRetryDoesNotSendSecondApproval() throws Exception {
        when(client().requestConfirm(anyInt(), anyString(), anyString())).thenThrow(
                new org.example.trusttrade.order.exception.ExternalApiException("timeout", new java.io.IOException()));
        confirm(15000, orderId, "pg-key").andExpect(status().isBadGateway());
        confirm(15000, orderId, "pg-key").andExpect(status().isAccepted()).andExpect(jsonPath("status").value("CONFIRMING"));
        verify(client(), times(1)).requestConfirm(anyInt(), anyString(), anyString());
        assertState(Payment.Status.CONFIRMING, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @Test void alreadyProcessedPaymentQueriesPgOutsideTransactionAndSynchronizes() throws Exception {
        doReturn(pg(400, "{\"code\":\"ALREADY_PROCESSED_PAYMENT\",\"message\":\"already\"}")).when(client()).requestConfirm(anyInt(), anyString(), anyString());
        var response = pg(200, done());
        when(client().checkState("pg-key")).thenAnswer(i -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()).isFalse();
            return response;
        });
        confirm(15000, orderId, "pg-key").andExpect(status().isOk()).andExpect(jsonPath("status").value("SUCCESS"));
        verify(client()).checkState("pg-key");
        assertState(Payment.Status.SUCCESS, Order.Status.PAID, ProductStatus.COMPLETED, 0);
    }
    @ParameterizedTest
    @ValueSource(strings={"not-json", "[]", "null", "{}", "{\"status\":null}", "{\"status\":\"\"}"})
    void malformedOrMissingStatusCannotFinalizePayment(String body) throws Exception {
        doReturn(pg(200, body)).when(client()).requestConfirm(anyInt(), anyString(), anyString());
        confirm(15000, orderId, "pg-key").andExpect(status().isBadGateway());
        assertState(Payment.Status.CONFIRMING, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @ParameterizedTest
    @ValueSource(strings={"orderId", "paymentKey", "totalAmount"})
    void successIdentityAndAmountMustMatchInApprovalAndSynchronization(String field) throws Exception {
        var body = json.readTree(done());
        ((com.fasterxml.jackson.databind.node.ObjectNode)body).put(field, "wrong");
        var response = pg(200, body.toString());
        when(client().requestConfirm(anyInt(), anyString(), anyString())).thenReturn(response);
        confirm(15000, orderId, "pg-key").andExpect(status().isBadGateway());
        assertThatThrownBy(() -> context.getBean(PaymentResultService.class).syncPaymentState("key", response))
                .isInstanceOf(org.example.trusttrade.order.exception.SystemException.PaymentSystemException.class);
        assertState(Payment.Status.CONFIRMING, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @ParameterizedTest
    @ValueSource(strings={"{}", "{\"code\":\"PROVIDER_ERROR\"}",
            "{\"code\":\"PROVIDER_ERROR\",\"message\":\"failed\"}", "[]"})
    void invalidErrorOrProviderFailureKeepsConfirming(String body) throws Exception {
        doReturn(pg(500, body)).when(client()).requestConfirm(anyInt(), anyString(), anyString());
        confirm(15000, orderId, "pg-key").andExpect(status().isBadGateway());
        assertState(Payment.Status.CONFIRMING, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @ParameterizedTest
    @ValueSource(strings={"amount", "orderId", "paymentKey", "idempotencyKey"})
    void missingRequiredInputReturns400BeforePgCall(String field) throws Exception {
        var body = new HashMap<String,Object>(Map.of("amount",15000,"orderId",orderId,
                "paymentKey","pg-key","idempotencyKey","key"));
        body.remove(field);
        mvc.perform(post("/payments/confirm").contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_REQUEST"));
        verifyNoInteractions(client());
        assertState(Payment.Status.READY, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @ParameterizedTest
    @ValueSource(strings={"amount-zero", "amount-negative", "blank-order", "blank-key", "blank-idem", "long-key", "null-key"})
    void invalidValuesReturn400(String scenario) throws Exception {
        var body = new HashMap<String,Object>(Map.of("amount",15000,"orderId",orderId,
                "paymentKey","pg-key","idempotencyKey","key"));
        switch (scenario) {
            case "amount-zero" -> body.put("amount", 0);
            case "amount-negative" -> body.put("amount", -1);
            case "blank-order" -> body.put("orderId", " ");
            case "blank-key" -> body.put("paymentKey", " ");
            case "blank-idem" -> body.put("idempotencyKey", " ");
            case "long-key" -> body.put("paymentKey", "x".repeat(201));
            case "null-key" -> body.put("paymentKey", null);
        }
        mvc.perform(post("/payments/confirm").contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client());
        assertState(Payment.Status.READY, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }
    @Test void validPositiveButWrongAmountIsRejectedBeforeApproval() throws Exception {
        confirm(100, orderId, "pg-key").andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("PAYMENT_AMOUNT_MISMATCH"));
        verifyNoInteractions(client());
        assertState(Payment.Status.READY, Order.Status.PENDING, ProductStatus.RESERVED, 0);
    }

    @Test void wrongOrderAndChangedKeyAreRejectedWithoutPgCall() throws Exception {
        confirm(15000, "wrong-order", "pg-key").andExpect(status().isBadRequest());
        context.getBean(PaymentResultService.class).prepareConfirm(15000, orderId, "pg-key", "key");
        confirm(15000, orderId, "different-key").andExpect(status().isBadRequest());
        verifyNoInteractions(client());
    }
    @Test void simultaneousRequestsSendOnlyOneApproval() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var response = pg(200, done());
        when(client().requestConfirm(anyInt(), anyString(), anyString())).thenAnswer(i -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("approval timeout");
            return response;
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = pool.submit(() -> confirm(15000, orderId, "pg-key").andReturn());
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<MvcResult> second = pool.submit(() -> confirm(15000, orderId, "pg-key").andReturn());
            MvcResult pending = second.get(5, TimeUnit.SECONDS);
            assertThat(pending.getResponse().getStatus()).isEqualTo(202);
            assertThat(json.readTree(pending.getResponse().getContentAsString()).path("status").asText()).isEqualTo("CONFIRMING");
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            verify(client(), times(1)).requestConfirm(anyInt(), anyString(), anyString());
            assertState(Payment.Status.SUCCESS, Order.Status.PAID, ProductStatus.COMPLETED, 0);
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
