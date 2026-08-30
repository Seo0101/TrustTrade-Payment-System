package org.example.trusttrade.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.trusttrade.auction.domain.AuctionOrder;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuctionOrderResDto {

    private String auctionOrderId;
    private int amount;
    private String auctionName;
    private UUID sellerId;
    private UUID buyerId;


    public AuctionOrderResDto(AuctionOrder auctionOrder) {
        this.auctionOrderId = auctionOrder.getId();
        this.amount = auctionOrder.getAmount();
        this.auctionName = auctionOrder.getProductName();
        this.sellerId = auctionOrder.getSeller().getId();
        this.buyerId = auctionOrder.getBuyer().getId();
    }
}
