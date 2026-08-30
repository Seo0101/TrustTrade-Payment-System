package org.example.trusttrade.auction.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.AuctionStatus;
import org.example.trusttrade.auction.domain.Bids;
import org.example.trusttrade.auction.domain.DepositOrder;
import org.example.trusttrade.auction.dto.AuctionItemDto;
import org.example.trusttrade.auction.repository.AuctionRepository;
import org.example.trusttrade.auction.repository.BidRepository;
import org.example.trusttrade.auction.repository.DepositOrderRepository;
import org.example.trusttrade.item.dto.StoredImage;
import org.example.trusttrade.item.service.ImageService;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ImageService imageService;
    private final DepositOrderRepository depositOrderRepository;
    private final DepositOrderService depositOrderService;

    // 경매 물품 등록
    @Transactional
    public void registerAuctionItem(AuctionItemDto auctionItemDto, User seller, List<MultipartFile> images) {
        List<StoredImage> storedImages = Collections.emptyList();
        try {

            // 이미지 변환
            storedImages = imageService.processImagesAndSetDto(images, auctionItemDto);

            // Auction 객체 생성
            Auction auction = Auction.fromDto(auctionItemDto, seller);
            auctionRepository.save(auction);

        } catch (Exception e) {
            imageService.deleteFiles(storedImages);
            throw new RuntimeException("경매 물품 등록 중 오류 발생", e);
        }

    }


    //경매 조죄 by sellerId
    @Transactional
    public List<Auction> getAuctionsBySeller(UUID sellerId) {
        return auctionRepository.getAuctionsBySellerId(sellerId);
    }

    //경매 목록 조회
    public List<Auction> getAuctions() {
        return auctionRepository.findAll();
    }

    //단일 경매 상세 정보 조회
    public Auction getAuctionById(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매가 존재하지 않습니다.  " + auctionId));
    }

    // 마감된 경매 처리
    @Transactional
    public void closeExpiredAuctions() throws IOException, InterruptedException {
        LocalDateTime nowKST = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<Auction> expiredAuctions = auctionRepository.findByEndTimeBeforeAndAuctionStatus(nowKST, AuctionStatus.OPEN);

        for (Auction auction : expiredAuctions) {
            bidRepository.findTopByAuctionOrderByBidPriceDesc(auction)
                    .ifPresent(bid -> auction.setWinner(bid.getUser()));

            auction.setAuctionStatus(AuctionStatus.CLOSED);

            //======경매 환불=========
            //refundDepositForLosingBidders(auction);

            System.out.println("Auction " + auction.getId() + " closed. Winner: " +
                    (auction.getWinner() != null ? auction.getWinner().getId() : "None"));



        }
    }

    //경매 마감시 보증금 환불
    @Transactional
    public void refundDepositForLosingBidders(Auction auction) throws IOException, InterruptedException {
        User winner = auction.getWinner();

        // 해당 경매의 모든 보증금 결제 내역 조회
        List<DepositOrder> deposits = depositOrderRepository.findByAuction(auction);

        for (DepositOrder deposit : deposits) {
            User depositor = deposit.getBidder();

            // 낙찰자 제외 + 이미 환불된 건 제외
            if (winner == null || !depositor.equals(winner)) {
                if (!deposit.isRefunded()) {
                    depositOrderService.cancelOrder(deposit.getId(), deposit.getPaymentKey(), "보증금 환불");
                    deposit.setRefunded(true);
                    depositOrderRepository.save(deposit);
                }
            }
        }
    }


}
