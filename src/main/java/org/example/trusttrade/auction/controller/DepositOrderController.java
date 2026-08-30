package org.example.trusttrade.auction.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.DepositOrder;
import org.example.trusttrade.auction.dto.DepositOrderPaymentResDto;
import org.example.trusttrade.auction.dto.DepositOrderReqDto;
import org.example.trusttrade.auction.service.DepositOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;


@RestController
@RequestMapping("/depositOrders")
@RequiredArgsConstructor
@Slf4j
public class DepositOrderController {

    @Autowired
    private final DepositOrderService depositOrderService;
    @Autowired
    private View error;

    @PostMapping("/new")
    public ResponseEntity<?> createDeposit(@RequestBody DepositOrderReqDto request) {

        try{
            DepositOrder depositOrder = depositOrderService.createDepositOrder(request);

            //orderId, amount, orderName 필수 설정
            DepositOrderPaymentResDto response = new DepositOrderPaymentResDto(
                    depositOrder.getId(),
                    depositOrder.getAmount(),
                    depositOrder.getAuctionName());
            return ResponseEntity.ok(response);

        }catch (IllegalArgumentException e) {
            log.warn("입력 오류 : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (EntityNotFoundException e) {
            log.info("리소스 없음 : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (Exception e) {
            log.error("예기치 못한 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

}
