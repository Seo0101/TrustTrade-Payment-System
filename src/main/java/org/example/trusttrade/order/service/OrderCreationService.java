package org.example.trusttrade.order.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.OrderStateException;
import org.example.trusttrade.order.exception.SystemException.DuplicateOrderException;
import org.example.trusttrade.order.exception.SystemException.OrderCreationFailedException;
import org.example.trusttrade.order.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.example.trusttrade.global.error.ErrorCode.ALREADY_PURCHASED;
import static org.example.trusttrade.order.util.DatabaseExceptionUtils.isDuplicateKeyException;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order create(
            String orderLockKey,
            Long productId,
            UUID buyerId
    ) {

        // 1. 여기서 상품 락 획득
        Product product = productRepository
                .findByIdForUpdate(productId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        // 2. 기존 요청 확인
        Optional<Order> existing =
                orderRepository.findByOrderLockKey(orderLockKey);


        if (existing.isPresent()) {
            Order existingOrder = existing.get();
            existingOrder.validateSameRequest(productId, buyerId);
            return handleExistingOrder(
                    existingOrder,
                    product,
                    buyerId
            );
        }

        // 3. 구매자 조회
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        User seller = product.getUser();

        // 4. 판매자가 확정한 구매자인지 검증
        product.validateOrderable(buyerId);

        //5. 같은 상품에 진행 중인 주문이 있으면 새 주문 생성 금지
        boolean hasActiveOrder = orderRepository.existsByProduct_IdAndStatusIn(
                productId,
                List.of(
                        Order.Status.CREATED,
                        Order.Status.PENDING,
                        Order.Status.FAILED
                )
        );

        if (hasActiveOrder) {
            throw new BusinessException(ErrorCode.PAYMENT_IN_PROGRESS);
        }


        // 6. 주문 생성
        Order order =
                Order.create(
                        product,
                        buyer,
                        seller,
                        orderLockKey
                );

        try {

            return orderRepository.saveAndFlush(order);

        } catch (DataIntegrityViolationException e) {

            if (isDuplicateKeyException(e)) {
                // 여기서 기존 Order를 조회하지 않음
                throw new DuplicateOrderException(
                        orderLockKey
                );
            }

            throw new OrderCreationFailedException(
                    "주문 생성 중 DB 무결성 오류",
                    e
            );
        }
    }

    //현재 상태의 주문을 반환해도 되는가?
    private Order handleExistingOrder(Order order, Product product, UUID buyerId) {
        switch (order.getStatus()) {
            case CREATED:
            case PENDING:
            case PAID:
            case COMPLETED:
                return order;

            case FAILED:
                product.validateRetryOrderable(buyerId);
                return order;

            case CANCELLED:
                //order에 멱등키가 있는데 cancelled 상태는 존재할 수 없음. => 방어 처리 로직 핑요힘
                //비지니스 예외가 아닌 5xx 서버 내부 예외로 발생시켜야 함.
                throw new OrderStateException(
                        "CANCELLED 주문에 order_lock_key가 존재합니다.");
        }
        throw new IllegalStateException("지원하지 않는 상태");
    }

    //dupli 복구 로직
    @Transactional
    public Order recoverExistingOrder(
            String orderLockKey,
            Long productId,
            UUID buyerId
    ) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        Order existingOrder = orderRepository.findByOrderLockKey(orderLockKey)
                .orElseThrow(() ->
                        new OrderCreationFailedException(
                                "중복 주문 충돌 후 기존 주문 조회 실패"
                        )
                );

        existingOrder.validateSameRequest(productId, buyerId);

        return handleExistingOrder(existingOrder, product, buyerId);
    }
}
