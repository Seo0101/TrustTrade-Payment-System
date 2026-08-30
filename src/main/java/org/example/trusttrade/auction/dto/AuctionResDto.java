package org.example.trusttrade.auction.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
@Getter
public class AuctionResDto {

    //Item
    private Long id;
    private UUID user;
    private String name;
    private String description;
    private LocalDateTime createdTime;
    //Auction
    private Integer startPrice;
    private Integer bidUnit;
    private AuctionStatus auctionStatus;
    private LocalDateTime endTime;
    private List<BidsResponseDto> bids;
    private UUID winner;



    public static AuctionResDto fromEntity(Auction auction) {

        List<BidsResponseDto> bidDtos = null;

        if (auction.getBids() != null && !auction.getBids().isEmpty()) {
            bidDtos = auction.getBids().stream()
                    .map(BidsResponseDto::from)
                    .collect(Collectors.toList());
        }

        UUID winnerId = auction.getWinner() != null ? auction.getWinner().getId() : null;

        return AuctionResDto.builder()
                .id(auction.getId())
                .user(auction.getUser() != null ? auction.getUser().getId() : null)
                .name(auction.getName())
                .description(auction.getDescription())
                .createdTime(auction.getCreatedTime())
                .startPrice(auction.getStartPrice())
                .bidUnit(auction.getBidUnit())
                .auctionStatus(auction.getAuctionStatus())
                .endTime(auction.getEndTime())
                .bids(bidDtos)
                .winner(winnerId)
                .build();

    }




}
