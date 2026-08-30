package org.example.trusttrade.auction.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.trusttrade.auction.dto.AuctionItemDto;
import org.example.trusttrade.item.domain.Item;
import org.example.trusttrade.item.domain.products.ProductLocation;
import org.example.trusttrade.login.domain.User;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder
@DiscriminatorValue("AUCTION")
@Table(name = "auction")
public class Auction extends Item {

    @Column(name = "start_price",nullable = false)
    private Integer startPrice;

    @Column(name = "bid_unit",nullable = false)
    private Integer bidUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_status",nullable = false)
    private AuctionStatus auctionStatus;

    @Column(name = "end_time",nullable = false)
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "auction")
    private List<Bids> bids;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    public static Auction fromDto(AuctionItemDto dto, User seller){
        // 위치 정보 생성/조회
        ProductLocation loc = ProductLocation.fromDto(dto);

        // 3) Auction 엔티티
        return Auction.builder()
                // Item
                .user(seller)
                .name(dto.getName())
                .description(dto.getDescription())
                .productLocation(loc)
                .createdTime(LocalDateTime.now())
                // Auction
                .startPrice(dto.getStartPrice())
                .bidUnit(dto.getBidUnit())
                .auctionStatus(AuctionStatus.OPEN)
                .endTime(dto.getEndTime())
                .build();
    }


    //경매 종료시, 낙찰자 설정 & 경매 상태 변경
    public void setWinner(User winner) {
        if (this.auctionStatus == AuctionStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 경매는 낙찰자를 변경할 수 없습니다.");
        }
        if (winner == null) throw new IllegalArgumentException("낙찰자는 null일 수 없습니다.");
        this.winner = winner;
    }

    //경매 상태 변경
    public void setAuctionStatus(AuctionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("경매 상태는 null일 수 없습니다.");
        }
        // 이미 종료된 경매 상태 변경 방지
        if (this.auctionStatus == AuctionStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 경매는 상태를 변경할 수 없습니다.");
        }

        this.auctionStatus = status;
    }

    //bid 연관관계 설정
    public void addBid(Bids bid) {bids.add(bid);}
}
