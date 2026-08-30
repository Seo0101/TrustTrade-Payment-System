package org.example.trusttrade.auction.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DepositOrderReqDto {

    private Long auctionId;
    private UUID buyerId;
    private UUID sellerId;
}
