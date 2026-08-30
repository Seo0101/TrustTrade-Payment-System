package org.example.trusttrade.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.AuctionOrder;
import org.example.trusttrade.auction.domain.Bids;
import org.example.trusttrade.auction.dto.AuctionOrderReqDto;
import org.example.trusttrade.auction.dto.AuctionResDto;
import org.example.trusttrade.auction.repository.AuctionOrderRepository;
import org.example.trusttrade.auction.repository.AuctionRepository;
import org.example.trusttrade.auction.repository.BidRepository;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.order.domain.Order;
import org.example.trusttrade.order.dto.OrderReqDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionOrderService {

    private final UserRepository userRepository;
    private final AuctionOrderRepository auctionOrderRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    //order 생성
    public AuctionOrder createOrder(AuctionOrderReqDto request) {

        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + request.getAuctionId()));

        User buyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found with id: " + request.getBuyerId()));

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + request.getSellerId()));

        Bids bid = bidRepository.findTopByAuctionOrderByBidPriceDesc(auction)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found"));


        AuctionOrder auctionOrder = AuctionOrder.create(auction, bid.getBidPrice(), buyer, seller);
        auctionOrderRepository.save(auctionOrder);

        return auctionOrder;
    }

    //주문 수령 완료
    public void completeOrder(String auctionOrderId) {
        AuctionOrder auctionOrder = auctionOrderRepository.findById(auctionOrderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        auctionOrder.completeOrder(); // 엔티티에서 상태 변경 메서드 호출

        // DB에 상태 변경 반영
        auctionOrderRepository.save(auctionOrder);
    }

    //주문 목록 조회
    public List<AuctionOrder> getAuctionOrdersByUserId(UUID userId){
        return auctionOrderRepository.getOrdersByUserId(userId);
    }
}
