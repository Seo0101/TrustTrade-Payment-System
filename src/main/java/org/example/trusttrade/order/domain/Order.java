package org.example.trusttrade.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.OrderStateUpdateException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.example.trusttrade.global.error.ErrorCode.PAYABLE_NOT_POSSIBLE;
import static org.example.trusttrade.global.error.ErrorCode.ORDER_IDEMPOTENCY_CONFLICT;
import static org.example.trusttrade.order.domain.Order.Status.*;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orders")
public class Order {

    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id" ,nullable = false)
    private Product product;

    private int amount;

    @Column(nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @OneToMany(mappedBy = "order")
    private List<Payment> payments = new ArrayList<Payment>();

    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private int retryCount;

    //failed = 재결제 가능, cancel = 재결제 불가능
    public enum Status{
        CREATED, PENDING, FAILED, PAID, COMPLETED, CANCELLED
    }

    //order 중복 요청 방지 키
    @Column(name = "order_lock_key", unique = true)
    private String orderLockKey;


    public void validateSameRequest(Long productId, UUID buyerId) {
        if (!this.product.getId().equals(productId)
                || !this.buyer.getId().equals(buyerId)) {
            throw new BusinessException(ORDER_IDEMPOTENCY_CONFLICT);
        }
    }

    public void increaseRetryCount(){
        this.retryCount++;
    }


   //payment 생성 후 추가
    public void addPayment(Payment payment){
        this.payments.add(payment);
        payment.setOrder(this);
    }

    //order 생성
    public static Order create(Product product, User buyer, User seller, String lockKey) {
        Order order = new Order();
        order.id = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        order.amount = product.getProductPrice();
        order.productName = product.getName();
        order.product = product;
        order.seller = seller;
        order.buyer = buyer;
        order.status = Status.CREATED;
        order.retryCount = 0;
        order.orderLockKey = lockKey;

        return order;
    }



    //결제 완료 상태
    public void paidOrder() {

        if (this.status != Status.PENDING) {
            throw new OrderStateUpdateException("PENDING 상태가 아니면 결제 완료 처리할 수 없습니다.");
        }
        this.status = Status.PAID;
    }
    // 수령 완료 상태로 변경
    public void completeOrder() {
        if (this.status != Status.PAID) {
            throw new OrderStateUpdateException("결제 완료 상태가 아니면 수령 완료 처리할 수 없습니다.");
        }
        this.status = Status.COMPLETED;
    }

    //재결제 불가능 처리
    public void cancelOrder() {

        if(this.status != Status.PENDING) {
            throw new OrderStateUpdateException("PENDING 상태가 아니면 취소 처리가 불가능합니다.");
        }
        this.orderLockKey = null;
        this.status = Status.CANCELLED;
    }

    //재결제 가능 처리
    public void failOrder() {
        if(this.status != Status.PENDING) {
            throw new OrderStateUpdateException("PENDING 상태가 아니면 취소 처리가 불가능합니다.");
        }
        this.status = Status.FAILED;
    }

    //결제 생성 전 주문 검증
    public void startPayment() {
        if (status != CREATED && status != FAILED) {
            throw new BusinessException(PAYABLE_NOT_POSSIBLE);
        }
        status = PENDING;
    }




}