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
class PaymentReadyApiIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private EntityManager em;
    private TransactionTemplate tx;
    private MockMvc mvc;
    private PaymentRepository payments;
    private String orderId;
    private final ObjectMapper json = new ObjectMapper();

    @Configuration
    @EnableTransactionManagement
    @Import({PaymentCreationService.class, PaymentService.class})
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
        @Bean PaymentResultService results() { return mock(PaymentResultService.class); }
        @Bean PaymentStateSyncService sync() { return mock(PaymentStateSyncService.class); }
    }

    @BeforeEach void start() {
        context = new AnnotationConfigApplicationContext(Config.class);
        em = context.getBean(EntityManager.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        payments = context.getBean(PaymentRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(context.getBean(PaymentService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        orderId = seedOrder();
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

    @Test void createsReadyPaymentWithOrderAmountAndPendingOrder() throws Exception {
        MvcResult result = request(orderId, "key").andExpect(status().isOk())
                .andExpect(jsonPath("status").value("READY"))
                .andExpect(jsonPath("idempotencyKey").value("key"))
                .andExpect(jsonPath("paymentId").isNumber()).andReturn();
        long id = responseId(result);
        assertThat(orderStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(count()).isEqualTo(1);
        int amount = tx.execute(s -> em.find(Payment.class, id).getAmount());
        assertThat(amount).isEqualTo(15000);
        verifyNoInteractions(context.getBean(TossPaymentClient.class));
    }

    @ParameterizedTest
    @EnumSource(Payment.Status.class)
    void sameKeyReturnsExistingPaymentAndItsCurrentStatus(Payment.Status state) throws Exception {
        long id = responseId(request(orderId, "key").andExpect(status().isOk()).andReturn());
        tx.executeWithoutResult(s -> {
            Payment payment = em.find(Payment.class, id);
            if (state != Payment.Status.READY) payment.changeStatus(Payment.Status.CONFIRMING);
            if (state == Payment.Status.SUCCESS) {
                payment.changeStatus(state);
                payment.getOrder().paidOrder();
                payment.getOrder().getProduct().setCompleted();
            } else if (state == Payment.Status.FAILED) {
                payment.changeStatus(state);
                payment.getOrder().failOrder();
            }
        });
        request(orderId, "key").andExpect(status().isOk()).andExpect(jsonPath("paymentId").value(id))
                .andExpect(jsonPath("status").value(state.name()));
        assertThat(count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"orderId\":\"x\"}", "{\"idempotencyKey\":\"x\"}",
            "{\"orderId\":null,\"idempotencyKey\":\"x\"}",
            "{\"orderId\":\"\",\"idempotencyKey\":\"x\"}",
            "{\"orderId\":\"  \",\"idempotencyKey\":\"x\"}",
            "{\"orderId\":\"x\",\"idempotencyKey\":null}",
            "{\"orderId\":\"x\",\"idempotencyKey\":\"\"}",
            "{\"orderId\":\"x\",\"idempotencyKey\":\"  \"}"})
    void invalidInputReturns400WithoutTouchingPersistence(String body) throws Exception {
        clearInvocations(payments);
        mvc.perform(post("/payments/ready").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_REQUEST"));
        verifyNoInteractions(payments);
        assertThat(orderStatus()).isEqualTo(Order.Status.CREATED);
        assertThat(count()).isZero();
    }

    @Test void differentKeyCannotStartSecondPaymentWhilePending() throws Exception {
        request(orderId, "key").andExpect(status().isOk());
        request(orderId, "new-key").andExpect(status().isConflict()).andExpect(jsonPath("code").value("P005"));
        assertThat(count()).isEqualTo(1);
    }

    @Test void differentOrderCannotReuseKey() throws Exception {
        request(orderId, "key").andExpect(status().isOk());
        String other = seedOrder();
        request(other, "key").andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("P007"));
        assertThat(count()).isEqualTo(1);
        Order.Status otherStatus = tx.execute(s -> em.find(Order.class, other).getStatus());
        assertThat(otherStatus).isEqualTo(Order.Status.CREATED);
    }

    @Test void failedOrderCanRetryWithNewKeyAndKeepsOldFailure() throws Exception {
        long oldId = responseId(request(orderId, "key").andExpect(status().isOk()).andReturn());
        tx.executeWithoutResult(s -> {
            Payment payment = em.find(Payment.class, oldId);
            payment.changeStatus(Payment.Status.CONFIRMING);
            payment.changeStatus(Payment.Status.FAILED);
            payment.getOrder().failOrder();
        });
        long newId = responseId(request(orderId, "retry-key").andExpect(status().isOk())
                .andExpect(jsonPath("status").value("READY")).andReturn());
        assertThat(newId).isNotEqualTo(oldId);
        assertThat(count()).isEqualTo(2);
        assertThat(orderStatus()).isEqualTo(Order.Status.PENDING);
        Payment.Status oldStatus = tx.execute(s -> em.find(Payment.class, oldId).getStatus());
        assertThat(oldStatus).isEqualTo(Payment.Status.FAILED);
    }

    @Test void invalidReservationRejectsBeforeChangingOrder() throws Exception {
        tx.executeWithoutResult(s -> em.find(Order.class, orderId).getProduct().resale());
        request(orderId, "key").andExpect(status().isConflict()).andExpect(jsonPath("code").value("P22"));
        assertThat(orderStatus()).isEqualTo(Order.Status.CREATED);
        assertThat(count()).isZero();
    }

    @Test void wrongReservedBuyerRejectsBeforeChangingOrder() throws Exception {
        tx.executeWithoutResult(s -> {
            Product product = em.find(Order.class, orderId).getProduct();
            product.resale();
            product.reserve(user());
        });
        request(orderId, "key").andExpect(status().isForbidden()).andExpect(jsonPath("code").value("P002"));
        assertThat(orderStatus()).isEqualTo(Order.Status.CREATED);
        assertThat(count()).isZero();
    }

    @Test void saveFailureRollsBackPendingTransition() throws Exception {
        doThrow(new DataIntegrityViolationException("injected non-duplicate persistence failure"))
                .when(payments).saveAndFlush(any(Payment.class));
        request(orderId, "key").andExpect(status().isInternalServerError());
        assertThat(orderStatus()).isEqualTo(Order.Status.CREATED);
        assertThat(count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void entityRejectsEmptyKeyEvenWithoutHttpValidation(String key) {
        Order order = tx.execute(s -> em.find(Order.class, orderId));
        assertThatThrownBy(() -> Payment.create(order, key))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("idempotencyKey는 필수입니다.");
        assertThat(count()).isZero();
    }

    @Test void concurrentSameKeyReturnsOnePaymentToBothRequests() throws Exception {
        List<MvcResult> results = concurrentRequests("same-key", "same-key");
        assertThat(results).allSatisfy(r -> assertThat(r.getResponse().getStatus()).isEqualTo(200));
        assertThat(responseId(results.get(0))).isEqualTo(responseId(results.get(1)));
        assertThat(count()).isEqualTo(1);
        verify(payments, times(2)).findByIdempotencyKeyForUpdate("same-key");
    }

    @Test void concurrentDifferentKeysCreateOnlyOnePayment() throws Exception {
        List<MvcResult> results = concurrentRequests("first", "second");
        assertThat(results.stream().map(r -> r.getResponse().getStatus()).toList())
                .containsExactlyInAnyOrder(200, 409);
        assertThat(count()).isEqualTo(1);
        assertThat(orderStatus()).isEqualTo(Order.Status.PENDING);
    }

    private List<MvcResult> concurrentRequests(String first, String second) throws Exception {
        // Both outer lookups must finish before either request enters createPayment().
        // This forces the race that the post-order-lock lookup is intended to handle.
        CyclicBarrier outerReads = new CyclicBarrier(2);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Optional<Payment> existing = em.createQuery(
                            "select p from Payment p where p.idempotencyKey = :key", Payment.class)
                    .setParameter("key", key).getResultList().stream().findFirst();
            outerReads.await(5, TimeUnit.SECONDS);
            return existing;
        }).when(payments).findByIdempotencyKey(anyString());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> a = pool.submit(() -> request(orderId, first).andReturn());
            Future<MvcResult> b = pool.submit(() -> request(orderId, second).andReturn());
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
