package org.example.trusttrade.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.OrderStateException;
import org.example.trusttrade.order.exception.SystemException.DuplicateOrderException;
import org.example.trusttrade.order.exception.SystemException.DuplicatePaymentException;
import org.example.trusttrade.order.exception.SystemException.OrderCreationFailedException;
import org.example.trusttrade.order.exception.SystemException.PaymentCreationFailedException;
import org.example.trusttrade.order.repository.OrderRepository;
import org.example.trusttrade.order.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.example.trusttrade.global.error.ErrorCode.ALREADY_PURCHASED;
import static org.example.trusttrade.order.util.DatabaseExceptionUtils.isDuplicateKeyException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;




    //다른 bean의 메서드를 호출하는 구조여야(ready -> createPayment) 트랜잭션 프록시가 정상 동작
    @Transactional
    public Payment createPayment(
            String orderId,
            String idempotencyKey
    ) {
        //비관적 락 적용
        //다른 멱등키로 중복요청 오는 경우, 이곳에서 동일한 주문에 대한 결제 생성 방지
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)) ;

        log.info("Creating payment for orderId = {} status = {}", orderId, order.getStatus());

        // 주문 락 대기 중 다른 요청이 결제를 생성했을 수 있으므로 재확인
        Optional<Payment> existing =
                paymentRepository.findByIdempotencyKeyForUpdate(idempotencyKey);

        if (existing.isPresent()) {
            Payment payment = existing.get();

            if (!payment.getOrder().getId().equals(orderId)) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_ORDER);
            }

            return payment;
        }

        // 예약 정합성 검증: 아래 4번
        order.getProduct()
                .validateRetryOrderable(order.getBuyer().getId());


        //CREATED / FAILED만 허용하고 PENDING으로 변경
        //동일 키 요청은 기존 결제를 반환하고,
        // 다른 키로 진행 중인 주문에 결제를 추가하려는 요청은 startPayment()에서 차단
        order.startPayment();

        Payment payment = Payment.create(
                order,
                idempotencyKey
        );

        try {
            //save()만 하면 커밋시점에 flush가 되서 try-catch 시점에서 예상한 예외가 발생하지 않을 수 있음
            return paymentRepository.saveAndFlush(payment);

        } catch (DataIntegrityViolationException e) {

            //충돌 예외
            if (isDuplicateKeyException(e)) {
                //위에서 결제 상태 변경, payment 생성한건 롤백됨
                throw new DuplicatePaymentException(idempotencyKey);
            }

            throw new PaymentCreationFailedException(
                    "결제 생성 중 DB 무결성 오류", e);
        }
    }
}
