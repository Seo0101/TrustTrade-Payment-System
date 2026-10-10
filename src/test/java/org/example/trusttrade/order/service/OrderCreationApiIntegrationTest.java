package org.example.trusttrade.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.example.trusttrade.global.error.GlobalExceptionHandler;
import org.example.trusttrade.item.domain.products.*;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.controller.OrderController;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
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

/** Real controller, service transaction proxies, JPA repositories and isolated H2 database. */
class OrderCreationApiIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private EntityManager em;
    private TransactionTemplate tx;
    private MockMvc mvc;
    private OrderRepository orders;
    private Long productId;
    private UUID buyerId;
    private final ObjectMapper json = new ObjectMapper();

    @Configuration
    @EnableTransactionManagement
    @Import({OrderCreationService.class, OrderService.class})
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
            var factory = new JpaRepositoryFactory(em);
            // H2 is only a test database. Normalize its unique violation to Spring's
            // DuplicateKeyException without adding H2-specific handling to production code.
            var dialect = new HibernateJpaDialect();
            org.springframework.dao.support.PersistenceExceptionTranslator translator = exception -> {
                for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                    if (cause instanceof java.sql.SQLException sql
                            && "23505".equals(sql.getSQLState())) {
                        return new org.springframework.dao.DuplicateKeyException(
                                "H2 test database unique constraint violation", exception);
                    }
                }
                // Foreign-key and other integrity violations remain ordinary database errors.
                return dialect.translateExceptionIfPossible(exception);
            };
            factory.addRepositoryProxyPostProcessor((proxy, metadata) -> proxy.addAdvice(
                    new org.springframework.dao.support.PersistenceExceptionTranslationInterceptor(translator)));
            return mock(OrderRepository.class, org.mockito.AdditionalAnswers.delegatesTo(
                    factory.getRepository(OrderRepository.class)));
        }
        @Bean ProductRepository products(EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(ProductRepository.class);
        }
        @Bean UserRepository users(EntityManager em) {
            return new JpaRepositoryFactory(em).getRepository(UserRepository.class);
        }
    }

    @BeforeEach void start() {
        context = new AnnotationConfigApplicationContext(Config.class);
        em = context.getBean(EntityManager.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        orders = context.getBean(OrderRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(
                        context.getBean(OrderService.class), mock(PaymentService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        tx.executeWithoutResult(s -> {
            User seller = user();
            User buyer = user();
            buyerId = buyer.getId();
            Product product = Product.builder().name("상품").description("설명").user(seller)
                    .productPrice(10000).status(ProductStatus.SALE).createdTime(LocalDateTime.now()).build();
            product.reserve(buyer);
            em.persist(product);
            em.flush();
            productId = product.getId();
        });
    }

    private User user() {
        User user = User.builder().role(User.Role.USER).memberType(User.MemberType.GENERAL)
                .roughAddress("서울").build();
        em.persist(user);
        return user;
    }

    @AfterEach void stop() { if (context != null) context.close(); }

    private ResultActions request(String key) throws Exception { return request(key, productId, buyerId); }
    private ResultActions request(String key, Long product, UUID buyer) throws Exception {
        return mvc.perform(post("/orders/new").contentType("application/json")
                .content(json.writeValueAsString(Map.of(
                        "orderLockKey", key, "productId", product, "buyerId", buyer))));
    }

    private String seedOrder(Order.Status status) {
        return tx.execute(s -> {
            Product product = em.find(Product.class, productId);
            Order order = Order.create(product, em.find(User.class, buyerId), product.getUser(), "key");
            if (status != Order.Status.CREATED) order.startPayment();
            switch (status) {
                case FAILED -> order.failOrder();
                case PAID, COMPLETED -> {
                    order.paidOrder();
                    product.setCompleted();
                    if (status == Order.Status.COMPLETED) order.completeOrder();
                }
                case CANCELLED -> { order.cancelOrder(); product.resale(); }
                default -> { }
            }
            em.persist(order);
            em.flush();
            return order.getId();
        });
    }

    @Test void newOrderReturnsCreatedAndKeepsReservation() throws Exception {
        request("key").andExpect(status().isOk()).andExpect(jsonPath("status").value("CREATED"))
                .andExpect(jsonPath("amount").value(10000)).andExpect(jsonPath("productName").value("상품"))
                .andExpect(jsonPath("orderId").isNotEmpty());
        assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(1L);
        assertThat(tx.<ProductStatus>execute(s -> em.find(Product.class, productId).getStatus())).isEqualTo(ProductStatus.RESERVED);
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = {"CREATED", "PENDING", "FAILED", "PAID", "COMPLETED"})
    void sameKeyReturnsSameOrderWithoutChangingStatus(Order.Status state) throws Exception {
        String id = seedOrder(state);
        request("key").andExpect(status().isOk()).andExpect(jsonPath("orderId").value(id))
                .andExpect(jsonPath("status").value(state.name()));
        assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(1L);
        assertThat(tx.<Order.Status>execute(s -> em.find(Order.class, id).getStatus())).isEqualTo(state);
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = {"CREATED", "PENDING", "FAILED"})
    void differentKeyCannotCreateSecondActiveOrder(Order.Status state) throws Exception {
        seedOrder(state);
        request("other-key").andExpect(status().isConflict()).andExpect(jsonPath("code").value("P008"));
        assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(1L);
    }

    @Test void anotherBuyerCannotReuseKey() throws Exception {
        seedOrder(Order.Status.CREATED);
        request("key", productId, UUID.randomUUID()).andExpect(status().isConflict())
                .andExpect(jsonPath("code").value("P24"));
    }

    @Test void unreservedProductCannotBeOrdered() throws Exception {
        tx.executeWithoutResult(s -> em.find(Product.class, productId).resale());
        request("key").andExpect(status().isConflict()).andExpect(jsonPath("code").value("P001"));
        assertThat(tx.<Long>execute(s -> orders.count())).isZero();
    }

    @Test void anotherBuyerCannotOrderReservedProduct() throws Exception {
        UUID other = tx.execute(s -> user().getId());
        request("key", productId, other).andExpect(status().isForbidden())
                .andExpect(jsonPath("code").value("P002"));
    }

    @Test void failedOrderRequiresValidReservation() throws Exception {
        seedOrder(Order.Status.FAILED);
        tx.executeWithoutResult(s -> em.find(Product.class, productId).resale());
        request("key").andExpect(status().isConflict()).andExpect(jsonPath("code").value("P22"));
    }

    @Test void cancelledOrderDoesNotPreventNewOrderAfterReconfirmation() throws Exception {
        String oldId = seedOrder(Order.Status.CANCELLED);
        tx.executeWithoutResult(s -> em.find(Product.class, productId).reserve(em.find(User.class, buyerId)));
        request("new-key").andExpect(status().isOk()).andExpect(jsonPath("status").value("CREATED"));
        assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(2L);
        assertThat(tx.<String>execute(s -> em.find(Order.class, oldId).getOrderLockKey())).isNull();
    }

    @Test void cancelledOrderWithKeyReturnsServerError() throws Exception {
        String id = seedOrder(Order.Status.CANCELLED);
        tx.executeWithoutResult(s -> em.createQuery("update Order o set o.orderLockKey = :key where o.id = :id")
                .setParameter("key", "key").setParameter("id", id).executeUpdate());
        request("key").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("code").value("ORDER_STATE_ERROR"));
    }

    @Test void realUniqueKeyFailureRollsBackThenRecoversExistingOrder() throws Exception {
        String id = seedOrder(Order.Status.CREATED);
        // Simulate pre-insert lookups missing a competing committed order; the DB constraint stays real.
        var reads = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> reads.getAndIncrement() == 0 ? Optional.empty()
                : em.createQuery("select o from Order o where o.orderLockKey = :key", Order.class)
                    .setParameter("key", "key").getResultStream().findFirst())
                .when(orders).findByOrderLockKey("key");
        doReturn(false).when(orders).existsByProduct_IdAndStatusIn(eq(productId), anyList());
        request("key").andExpect(status().isOk()).andExpect(jsonPath("orderId").value(id))
                .andExpect(jsonPath("status").value("CREATED"));
        verify(orders, times(2)).findByOrderLockKey("key");
        verify(orders).saveAndFlush(any(Order.class));
        assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(1L);
    }

    @Test void concurrentDifferentKeysCreateOnlyOneOrder() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String key : List.of("first", "second")) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!go.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return request(key).andReturn().getResponse().getStatus();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(tx.<Long>execute(s -> orders.count())).isEqualTo(1L);
        } finally { go.countDown(); pool.shutdownNow(); }
    }
}
