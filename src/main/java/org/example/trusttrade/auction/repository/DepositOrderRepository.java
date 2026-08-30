package org.example.trusttrade.auction.repository;

import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.DepositOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositOrderRepository extends JpaRepository<DepositOrder, String> {

    List<DepositOrder> findByAuction(Auction auction);
}
