package org.example.trusttrade.auction.repository;

import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.Bids;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bids, Long> {

    // 금액 기준으로 Top 5 조회
    List<Bids> findTop5ByAuctionIdOrderByBidPriceDesc(Long auctionId);

    //특정 경먀의 입찰 금액 가장 높은 입찰 조회
    Optional<Bids> findTopByAuctionOrderByBidPriceDesc(Auction auction);
}
