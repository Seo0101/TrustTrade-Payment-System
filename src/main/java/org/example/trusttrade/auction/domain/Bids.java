package org.example.trusttrade.auction.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.trusttrade.login.domain.User;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Bids {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="bids_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id",nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id",nullable = false)
    private User user;

    @Column(name ="bid_price" ,nullable = false)
    private int bidPrice;

    @CreationTimestamp
    @Column(name = "created_time",nullable = false)
    private LocalDateTime createdTime;

    public static Bids create(Auction auction, User user, int bidPrice) {
        Bids bid = Bids.builder()
                .auction(auction)
                .user(user)
                .bidPrice(bidPrice)
                .build();

        //경매에 입찰자 등록
        auction.addBid(bid);
        return bid;
    }

}
