package org.example.trusttrade.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.AuctionOrder;
import org.example.trusttrade.auction.repository.AuctionOrderRepository;
import org.example.trusttrade.notification.service.NotificationService;
import org.example.trusttrade.order.client.TossPaymentClient;
import org.example.trusttrade.order.dto.ConfirmPaymentRequest;
import org.example.trusttrade.order.exception.OrderCancellationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuctionPaymentService {

    private final AuctionOrderRepository auctionOrderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final NotificationService notificationService;

    //결제 정보 검증
    public AuctionOrder verifyPayment(String orderId, int amount) throws OrderCancellationException {

        AuctionOrder find = auctionOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다." + orderId));

        if (find.getAmount() == amount) {
            return find;
        } else {
            return null;
        }
    }

    //결제 정보 인증 api 호출
    public boolean confirmAndSavePayment(ConfirmPaymentRequest request) throws IOException, InterruptedException {

        int amount = request.getAmount();
        String orderId = request.getOrderId();
        String paymentKey = request.getPaymentKey();

        //요청과 승인 사이 결제 금액 무결성 검증
        AuctionOrder auctionOrder = verifyPayment(orderId, amount);

        HttpResponse<String> response = tossPaymentClient.requestConfirm(amount, orderId, paymentKey);
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode == 200) {
            try {
                //payment키 설정
                auctionOrder.setPaymentKey(request.getPaymentKey());
                // 주문 조회 및 상태 변경
                processOrderAndPayment(request.getOrderId(), true); // 결제 성공 시 처리
                //알림 처리
                notificationService.createNotification(
                        "결제가 완료되었습니다. 배송을 시작해주세요.", auctionOrder.getSeller().getId());
                notificationService.createNotification(
                        "결제가 완료되었습니다.", auctionOrder.getBuyer().getId());
                return true;

            } catch (Exception e) {
                //주문 취소
                HttpResponse cancelResponse = tossPaymentClient.requestPaymentCancel(
                        request.getPaymentKey(),
                        "결제 승인 후 DB 저장 실패로 인한 자동 취소");
                //상태 변경(paid -> cancle)
                processOrderAndPayment(request.getOrderId(), false);
                //알림 처리
                notificationService.createNotification(
                        "결제 정보 저장 실패로 결제가 최소되었습니다. 다시 결제를 시도해주세요", auctionOrder.getBuyer().getId());
                throw new OrderCancellationException("db 저장 실패로 결제 취소 " + cancelResponse.body());
            }

        } else {
            // Toss 응답 실패 (ex. 결제 키 오류, 금액 불일치 등)
            //알림처리
            notificationService.createNotification(
                    "결제 실패하였습니다. 다시 결제를 시도해주세요", auctionOrder.getBuyer().getId());
            throw new OrderCancellationException("결제 승인 실패: " + responseBody);
        }
    }


    // 주문과 결제 상태 변경을 처리하는 메서드
    private void processOrderAndPayment(String orderId, boolean isSuccess) {
        try {
            // 주문 조회 및 상태 변경 >> 테스트용 주문 id 따로 설정해서 사용해야함
            AuctionOrder auctionOrder = auctionOrderRepository.findById(orderId).orElse(null);
            if (isSuccess) {
                auctionOrder.paidOrder();  // 결제 성공 시 주문 상태 변경
            } else {
                auctionOrder.cancel();  // 결제 실패 시 주문 상태 변경
            }
            auctionOrderRepository.save(auctionOrder);

        } catch (Exception ex) {
            throw new IllegalStateException("주문 또는 결제 상태 변경 중 오류 발생: " + ex.getMessage(), ex);
        }
    }

}
