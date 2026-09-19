package org.example.trusttrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.trusttrade.item.domain.products.*;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.domain.*;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.repository.*;
import org.example.trusttrade.order.scheduler.PaymentSyncScheduler;
import org.example.trusttrade.order.util.PaymentResponseParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentTransactionIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private TransactionTemplate tx;
    private EntityManager em;

    @Configuration
    @EnableTransactionManagement
    @Import({PaymentCreationService.class, PaymentResultService.class,
            PaymentStateSyncService.class, PaymentSyncScheduler.class, PaymentResponseParser.class})
    static class Config {
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(new DriverManagerDataSource(
                    "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
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
            return new JpaRepositoryFactory(em).getRepository(PaymentRepository.class);
        }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean TossPaymentClient toss() { return mock(TossPaymentClient.class); }
        @Bean PaymentService paymentService() { return mock(PaymentService.class); }
    }

    @BeforeEach void start() {
        context = new AnnotationConfigApplicationContext(Config.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        em = context.getBean(EntityManager.class);
    }
    @AfterEach void stop() { if (context != null) context.close(); }

    private Order seedOrder() {
        return tx.execute(status -> {
            User buyer = User.builder().role(User.Role.USER).memberType(User.MemberType.GENERAL)
                    .roughAddress("서울").build();
            User seller = User.builder().role(User.Role.USER).memberType(User.MemberType.GENERAL)
                    .roughAddress("서울").build();
            em.persist(buyer); em.persist(seller);
            Product product = Product.builder().name("상품").description("설명").user(seller)
                    .productPrice(10000).status(ProductStatus.SALE).createdTime(LocalDateTime.now()).build();
            product.reserve(buyer);
            em.persist(product);
            Order order = Order.create(product, buyer, seller, UUID.randomUUID().toString());
            em.persist(order);
            return order;
        });
    }

    @Test void differentKeysCannotCreateTwoPaymentsWhileOrderIsLocked() throws Exception {
        Order order = seedOrder();
        var creator = context.getBean(PaymentCreationService.class);
        var repository = context.getBean(OrderRepository.class);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> tx.executeWithoutResult(status -> {
                repository.findByIdForUpdate(order.getId()).orElseThrow();
                locked.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
                creator.createPayment(order.getId(), "first");
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = pool.submit(() -> {
                started.countDown();
                return creator.createPayment(order.getId(), "second");
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(BusinessException.class);
            tx.executeWithoutResult(status -> {
                assertThat(em.createQuery("select count(p) from Payment p", Long.class).getSingleResult()).isEqualTo(1L);
                assertThat(em.find(Order.class, order.getId()).getStatus()).isEqualTo(Order.Status.PENDING);
            });
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    private Payment seedConfirming(boolean invalidProduct) {
        Order order = seedOrder();
        return tx.execute(status -> {
            Order managed = em.find(Order.class, order.getId());
            managed.startPayment();
            Payment payment = Payment.create(managed, UUID.randomUUID().toString());
            payment.assignPaymentKey(UUID.randomUUID().toString());
            payment.changeStatus(Payment.Status.CONFIRMING);
            em.persist(payment);
            if (invalidProduct) managed.getProduct().setCompleted();
            return payment;
        });
    }

    @Test void schedulerCallsPgOutsideTransactionAndRollsBackOnlyFailedPayment() {
        Payment failed = seedConfirming(true);
        Payment successful = seedConfirming(false);
        when(context.getBean(PaymentService.class).syncConfirmingPayments(any()))
                .thenReturn(List.of(failed, successful));
        TossPaymentClient client = context.getBean(TossPaymentClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"status\":\"DONE\"}");
        when(client.checkState(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response;
        });
        context.getBean(PaymentSyncScheduler.class).syncPayments();
        verify(client, times(2)).checkState(anyString());
        tx.executeWithoutResult(status -> {
            assertThat(em.find(Payment.class, failed.getId()).getStatus()).isEqualTo(Payment.Status.CONFIRMING);
            assertThat(em.find(Order.class, failed.getOrder().getId()).getStatus()).isEqualTo(Order.Status.PENDING);
            assertThat(em.find(Payment.class, successful.getId()).getStatus()).isEqualTo(Payment.Status.SUCCESS);
            assertThat(em.find(Order.class, successful.getOrder().getId()).getStatus()).isEqualTo(Order.Status.PAID);
        });
    }
}
