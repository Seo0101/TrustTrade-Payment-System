package org.example.trusttrade.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.item.service.ProductService;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.exception.*;
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
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderCreationService orderCreationService;


    //order 생성
    public Order createOrder(String orderLockKey, Long productId, UUID buyerId) {

        try {

            //Transaction 1
            return orderCreationService.create(
                    orderLockKey,
                    productId,
                    buyerId
            );

        } catch (DuplicateOrderException e) {

            //유니크 제약조건 예외 발생시 복구 로직
            //tx 1은 끝난 상태
            Order existingOrder =
                    orderRepository.findByOrderLockKey(orderLockKey)
                            .orElseThrow(() ->
                                    new OrderCreationFailedException(
                                            "중복 주문 충돌 후 기존 주문 조회 실패",
                                            e
                                    )
                            );

            existingOrder.validateSameRequest(productId, buyerId);
            return existingOrder;
        }
    }

    //주문 수령 완료
    public void completeOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. orderId : " + orderId));

        order.completeOrder(); // 엔티티에서 상태 변경 메서드 호출

        // DB에 상태 변경 반영
        orderRepository.save(order);
    }


    //주문 목록 조회
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.getOrdersByUserId(userId);
    }


}
