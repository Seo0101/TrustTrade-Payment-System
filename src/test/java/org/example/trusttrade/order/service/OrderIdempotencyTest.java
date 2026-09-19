package org.example.trusttrade.order.service;

import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.SystemException.DuplicateOrderException;
import org.example.trusttrade.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderIdempotencyTest {
    private static final String KEY = "existing-order-key";
    private static final Long PRODUCT_ID = 1L;
    private final UUID buyerId = UUID.randomUUID();
    private OrderRepository orders;
    private ProductRepository products;
    private UserRepository users;
    private OrderCreationService creation;
    private OrderService service;
    private Order existing;

    enum Path { EXISTING_ORDER, DUPLICATE_RECOVERY }

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        products = mock(ProductRepository.class);
        users = mock(UserRepository.class);
        creation = mock(OrderCreationService.class);
        service = new OrderService(orders, creation);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(PRODUCT_ID);
        when(product.getProductPrice()).thenReturn(10000);
        when(product.getName()).thenReturn("상품");
        User buyer = mock(User.class);
        when(buyer.getId()).thenReturn(buyerId);
        existing = Order.create(product, buyer, mock(User.class), KEY);
        when(orders.findByOrderLockKey(KEY)).thenReturn(Optional.of(existing));
    }

    private Order request(Path path, Long productId, UUID requestedBuyer) {
        if (path == Path.EXISTING_ORDER) {
            Product requestedProduct = mock(Product.class);
            when(products.findByIdForUpdate(productId)).thenReturn(Optional.of(requestedProduct));
            return new OrderCreationService(orders, products, users)
                    .create(KEY, productId, requestedBuyer);
        }
        when(creation.create(KEY, productId, requestedBuyer))
                .thenThrow(new DuplicateOrderException(KEY));
        return service.createOrder(KEY, productId, requestedBuyer);
    }

    @ParameterizedTest
    @EnumSource(Path.class)
    void sameRequestReturnsExistingOrder(Path path) {
        assertThat(request(path, PRODUCT_ID, buyerId)).isSameAs(existing);
        verify(orders, never()).saveAndFlush(any(Order.class));
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @EnumSource(Path.class)
    void differentProductRejectsKeyReuse(Path path) {
        assertConflict(path, 2L, buyerId);
    }

    @ParameterizedTest
    @EnumSource(Path.class)
    void differentBuyerRejectsKeyReuse(Path path) {
        assertConflict(path, PRODUCT_ID, UUID.randomUUID());
    }

    private void assertConflict(Path path, Long productId, UUID requestedBuyer) {
        assertThatThrownBy(() -> request(path, productId, requestedBuyer))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT));
        verify(orders, never()).saveAndFlush(any(Order.class));
        verifyNoInteractions(users);
        assertThat(existing.getStatus()).isEqualTo(Order.Status.CREATED);
    }
}
