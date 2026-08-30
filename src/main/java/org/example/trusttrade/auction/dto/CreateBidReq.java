package org.example.trusttrade.auction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreateBidReq {

    @NotNull
    UUID user;
    @NotNull
    Long auctionId;
    @NotNull
    int bidPrice;
}
