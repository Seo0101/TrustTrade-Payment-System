package org.example.trusttrade.auction.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.DepositOrder;
import org.example.trusttrade.auction.dto.DepositOrderReqDto;
import org.example.trusttrade.auction.repository.AuctionRepository;
import org.example.trusttrade.auction.repository.DepositOrderRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.notification.service.NotificationService;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.dto.ConfirmPaymentRequest;
import org.example.trusttrade.order.exception.OrderCancellationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class DepositOrderService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final NotificationService notificationService;

    //depositOrder 생성
    public DepositOrder createDepositOrder(DepositOrderReqDto request) {

        //user 객체 id 검색
        List<User> users = userRepository.findAll();
        for (User user : users) {
            System.out.println(user.getId());
        }

        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new EntityNotFoundException("Auction not found"));

        User bidder = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new EntityNotFoundException("Buyer not found with id: " + request.getBuyerId()));

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new EntityNotFoundException("Seller not found with id: " + request.getSellerId()));

        if(!auction.getUser().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("seller does not match the auction owner.");
        }
        DepositOrder depositOrder = DepositOrder.create(auction, bidder, seller);
        depositOrderRepository.save(depositOrder);

        return depositOrder;
    }

    //결제 정보 검증 > confirm에서 같이 해도 되는거 아닌가
    public DepositOrder verifyPaymentDeposit(int amount, String orderId) throws OrderCancellationException {

        DepositOrder find = depositOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));

        if (find.getAmount() == amount) {
            return find;
        } else {
            return null;
        }
    }

    //결제 정보 인증 api 호출
    public boolean confirmAndSavePayment(int amount, String orderId, String paymentKey) throws IOException, InterruptedException {

        //요청과 승인 사이 결제 금액 무결성 검증
        DepositOrder order = verifyPaymentDeposit(amount, orderId);

        HttpResponse<String> response = tossPaymentClient.requestConfirm(amount, orderId, paymentKey);
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode == 200) {
            try {
                //payment키 설정
                order.setPaymentKey(paymentKey);
                // 주문 조회 및 상태 변경
                processOrderAndPayment(orderId, true); // 결제 성공 시 처리
                //알림 처리
                notificationService.createNotification(
                        order.getAuctionName() + " 보증금 결제가 완료되었습니다.", order.getBidder().getId());
                return true;

            } catch (Exception e) {
                //주문 취소
                HttpResponse cancelResponse = tossPaymentClient.requestPaymentCancel(
                        paymentKey,
                        "결제 승인 후 DB 저장 실패로 인한 자동 취소");
                //상태 변경(paid -> cancle)
                processOrderAndPayment(orderId, false);
                //알림 처리
                notificationService.createNotification(
                        "결제 정보 저장 실패로 결제가 최소되었습니다. 다시 결제를 시도해주세요", order.getBidder().getId());
                throw new OrderCancellationException("db 저장 실패로 결제 취소 " + cancelResponse.body());
            }

        } else {
            // Toss 응답 실패 (ex. 결제 키 오류, 금액 불일치 등)
            //알림처리
            notificationService.createNotification(
                    "결제 실패하였습니다. 다시 결제를 시도해주세요", order.getBidder().getId());
            throw new OrderCancellationException("결제 승인 실패: " + responseBody);
        }
    }


    // 주문과 결제 상태 변경을 처리하는 메서드
    private void processOrderAndPayment(String orderId, boolean isSuccess) {
        try {
            // 주문 조회 및 상태 변경 >> 테스트용 주문 id 따로 설정해서 사용해야함
            DepositOrder order = depositOrderRepository.findById(orderId).orElse(null);
            if (isSuccess) {
                order.paidDepositOrder();  // 결제 성공 시 주문 상태 변경
            } else {
                order.cancelDepositOrder();  // 결제 실패 시 주문 상태 변경
            }
            depositOrderRepository.save(order);

        } catch (Exception ex) {
            throw new IllegalStateException("주문 또는 결제 상태 변경 중 오류 발생: " + ex.getMessage(), ex);
        }
    }


    //주문 취소 및 상태 변경(환불 시 사용) - 보류
    public DepositOrder cancelOrder(String orderId, String paymentKey, String reason) throws IOException, InterruptedException {

        depositOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문 정보가 존재하지 않습니다."));

        HttpResponse response = tossPaymentClient.requestPaymentCancel(paymentKey, reason);

        if (response.statusCode() == 200) {
            processOrderAndPayment(orderId, false);
            return depositOrderRepository.findById(orderId).orElse(null);
        } else {
            throw new OrderCancellationException("보증금 주문 취소에 실패하였습니다. 다시 시도해주세요 : " + response.body());
        }

    }
}




