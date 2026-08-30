package org.example.trusttrade.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.AuctionStatus;
import org.example.trusttrade.auction.domain.Bids;
import org.example.trusttrade.auction.repository.AuctionRepository;
import org.example.trusttrade.auction.repository.BidRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    @Autowired private final BidRepository bidRepository;
    @Autowired private final AuctionRepository auctionRepository;
    @Autowired private final UserRepository userRepository;

    //bid 생성
    public Bids creatBid(UUID bidderId, Long auctionId, int bidPrice){
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Id의 사용자를 찾을 수 없습니다."));

        if(auction.getAuctionStatus() != AuctionStatus.OPEN){
            throw new IllegalStateException("종료된 경매에선 입찰에 참여할 수 없습니다.");
        }
        Bids bid  = Bids.create(auction, bidder, bidPrice);
        bidRepository.save(bid);


        return bid;
    }

    //top5 입찰 조회
    public List<Bids> getTop5BidsByAuction(Long auctionId) {
        auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 ID의 경매를 찾을 수 없습니다. " + auctionId));

        return bidRepository.findTop5ByAuctionIdOrderByBidPriceDesc(auctionId);
    }

}
