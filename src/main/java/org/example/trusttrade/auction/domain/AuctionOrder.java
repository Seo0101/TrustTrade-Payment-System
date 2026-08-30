package org.example.trusttrade.auction.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.order.domain.Order;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "auctionOrders")
public class AuctionOrder {
        @Id
        //@GeneratedValue(strategy = GenerationType.AUTO)
        @Column(name = "auction_order_id")
        private String id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "auction_id" ,nullable = false)
        private Auction auction;

        private int amount;

        @Column(unique = true)
        private String paymentKey;

        @Column(nullable = false)
        private String productName;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "seller_id", nullable = false)
        private User seller;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "buyer_id", nullable = false)
        private User buyer;

        @Enumerated(EnumType.STRING)
        private Status status = Status.PENDING;

        @CreatedDate
        private LocalDateTime createdAt;
        @LastModifiedDate
        private LocalDateTime updatedAt;

        public enum Status{
            PENDING, PAID, COMPLETED, CANCELLED
        }

        //order 생성
        public static AuctionOrder create(Auction auction, int amount, User buyer, User seller) {
            AuctionOrder auctionOrder = new AuctionOrder();
            auctionOrder.id = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
            auctionOrder.amount = amount;
            auctionOrder.productName = auction.getName();
            auctionOrder.auction = auction;
            auctionOrder.seller = seller;
            auctionOrder.buyer = buyer;

            return auctionOrder;
        }

        //결제 인증 api 호출후 paymentKey 설정
        public void setPaymentKey(String paymentKey) {
            this.paymentKey = paymentKey;
        }


        //결제 완료 상태
        public void paidOrder() {
            if (this.status != Status.PENDING) {
                throw new IllegalStateException("PENDING 상태가 아니면 결제 완료 처리할 수 없습니다.");
            }
            this.status = Status.PAID;
        }
        // 수령 완료 상태로 변경
        public void completeOrder() {
            if (this.status != Status.PAID) {
                throw new IllegalStateException("결제 완료 상태가 아니면 수령 완료 처리할 수 없습니다.");
            }
            this.status = Status.COMPLETED;
        }

        //결제 취소될 경우 상태
        public void cancel() {
            if(this.status != Status.PAID) {
                throw new IllegalStateException("PAID 상태가 아니면 취소 처리가 불가능합니다.");
            }
            this.status = Status.CANCELLED;
        }
}
