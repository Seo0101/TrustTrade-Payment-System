package org.example.trusttrade.auction.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.trusttrade.login.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class DepositOrder {

    @Id
    @Column(name = "deposit_order_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    private int amount;

    @Column(unique = true)
    private String paymentKey;

    @Column(nullable = false)
    private String auctionName;

    @ManyToOne
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    //insert시 자동 생성
    @CreatedDate
    private LocalDateTime createdAt;
    //업데이트시 자동 생성
    @LastModifiedDate
    private LocalDateTime updateAt;

    public enum Status{
        PENDING, DEPOSITED, REFUNDED, CANCELED
    }

    public static DepositOrder create(Auction auction, User bidder, User seller) {
        DepositOrder depositOrder = new DepositOrder();
        depositOrder.id = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        depositOrder.amount = 20000;
        depositOrder.auctionName = auction.getName() + " 보증금 결제";
        depositOrder.auction = auction;
        depositOrder.seller = seller;
        depositOrder.bidder = bidder;

        return depositOrder;
    }

    //결제 인증 api 호출후 paymentKey 설정
    public void setPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    //결제 완료 상태
    public void paidDepositOrder() {
        if (this.status != DepositOrder.Status.PENDING) {
            throw new IllegalStateException("PENDING 상태가 아니면 결제 완료 처리할 수 없습니다.");
        }
        this.status = DepositOrder.Status.DEPOSITED;
    }

    //결제 취소될 경우 상태
    public void cancelDepositOrder() {
        if(this.status != DepositOrder.Status.DEPOSITED) {
            throw new IllegalStateException("DEPOSITED 상태가 아니면 취소 처리가 불가능합니다.");
        }
        this.status = DepositOrder.Status.CANCELED;
    }
    //환불 진행 중 사용
    public boolean isRefunded(){
        if(this.status == DepositOrder.Status.REFUNDED) {
            return true;
        }
        else return false;
    }
    //상태 환불 처리
    public void setRefunded(boolean refunded) {
        if(this.status != DepositOrder.Status.REFUNDED) {
            this.status = DepositOrder.Status.REFUNDED;
        }
    }


}
