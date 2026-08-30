package org.example.trusttrade.auction.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter @Setter
public class AuctionOrderReqDto {

    private Long auctionId;
    private UUID buyerId;
    private UUID sellerId;

}
