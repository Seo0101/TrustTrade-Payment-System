package org.example.trusttrade.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.domain.Payment;
import org.example.trusttrade.order.dto.OrderPaymentResDto;
import org.example.trusttrade.order.dto.OrderReqDto;
import org.example.trusttrade.order.dto.OrderResDto;
import org.example.trusttrade.order.dto.PaymentStatusResponse;
import org.example.trusttrade.order.service.OrderService;
import org.example.trusttrade.order.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    @Autowired
    private final OrderService orderService;
    @Autowired
    private final PaymentService paymentService;

    //결제 상태 조회 api
    @GetMapping("/{orderId}/payment-status")
    public PaymentStatusResponse getPaymentStatus(
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey) {

        Payment payment = paymentService.getPaymentStatus(orderId, idempotencyKey);

        return PaymentStatusResponse.from(payment);
    }

    //order 생성
    @PostMapping("/new")
    public ResponseEntity<OrderPaymentResDto> createOrder(@RequestBody OrderReqDto request) {

        Order order = orderService.createOrder(request.getOrderLockKey() ,request.getProductId(), request.getBuyerId());

        //필수 설정 : 1.amount - currency, value 2.orderId 3.orderName
        OrderPaymentResDto response = new OrderPaymentResDto(
                order.getId(),
                order.getAmount(),
                order.getProductName());

        return ResponseEntity.ok(response);
    }

    //상품 수령시 order 상태 변경
    // 수령 완료 클릭 시 주문 상태를 COMPLETED로 변경
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable("orderId") String orderId) {
        try {
            orderService.completeOrder(orderId);
            return ResponseEntity.ok("주문 수령 완료 처리되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    //구매 목록 조회 by userId
    @GetMapping("/{userId}/list")
    public ResponseEntity<?> getOrders(@PathVariable("userId") UUID userId) {

        List<Order> orders = orderService.getOrdersByUserId(userId);
        List<OrderResDto> response = orders.stream()
                .map(OrderResDto::new)
                .toList();

        return ResponseEntity.ok(response);

    }


}
