package org.example.trusttrade.auction.repository;

import org.example.trusttrade.auction.domain.AuctionOrder;
import org.example.trusttrade.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, String> {


    @Query("SELECT o FROM AuctionOrder o WHERE o.buyer.id = :userId")
    List<AuctionOrder> getOrdersByUserId(@Param("userId") UUID userId);

}
