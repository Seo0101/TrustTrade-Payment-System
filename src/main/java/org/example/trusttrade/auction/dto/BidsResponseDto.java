package org.example.trusttrade.auction.dto;

import lombok.*;
import org.example.trusttrade.auction.domain.Bids;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@Builder
public class BidsResponseDto {
    private Long id;
    private UUID bidderId;
    private int bidPrice;
    private LocalDateTime createdTime;

    public static BidsResponseDto from(Bids bid) {

        return  BidsResponseDto.builder()
                .id(bid.getId())
                .bidderId(bid.getUser().getId())
                .bidPrice(bid.getBidPrice())
                .createdTime(bid.getCreatedTime())
                .build();

    }
}

