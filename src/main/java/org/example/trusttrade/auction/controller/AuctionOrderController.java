package org.example.trusttrade.auction.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.domain.AuctionOrder;
import org.example.trusttrade.auction.dto.AuctionOrderPaymentResDto;
import org.example.trusttrade.auction.dto.AuctionOrderReqDto;
import org.example.trusttrade.auction.dto.AuctionOrderResDto;
import org.example.trusttrade.auction.service.AuctionOrderService;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auctionOrders")
@RequiredArgsConstructor
public class AuctionOrderController {

    private final UserRepository userRepository;
    private final AuctionOrderService auctionOrderService;


    //order 생성
    @PostMapping("/new")
    public ResponseEntity<AuctionOrderPaymentResDto> createAuctionOrder(@RequestBody AuctionOrderReqDto request) {

        //user 객체 id 검색
        List<User> users = userRepository.findAll();
        for (User user : users) {
            System.out.println(user.getId());
        }

        AuctionOrder auctionOrder = auctionOrderService.createOrder(request);

        //필수 설정 : 1.amount - currency, value 2.orderId 3.orderName
        AuctionOrderPaymentResDto response = new AuctionOrderPaymentResDto(
                auctionOrder.getId(),
                auctionOrder.getAmount(),
                auctionOrder.getProductName());

        return ResponseEntity.ok(response);
    }

    //상품 수령시 order 상태 변경
    // 수령 완료 클릭 시 주문 상태를 COMPLETED로 변경
    @PostMapping("/{auctionOrderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable("auctionOrderId") String orderId) {
        try {
            auctionOrderService.completeOrder(orderId);
            return ResponseEntity.ok("주문 수령 완료 처리되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    //구매 목록 조회 by userId
    @GetMapping("/{userId}/list")
    public ResponseEntity<?> getOrders(@PathVariable("userId") UUID userId) {

        List<AuctionOrder> auctionOrders = auctionOrderService.getAuctionOrdersByUserId(userId);
        List<AuctionOrderResDto> response = auctionOrders.stream()
                .map(AuctionOrderResDto::new)
                .toList();

        return ResponseEntity.ok(response);

    }
}
