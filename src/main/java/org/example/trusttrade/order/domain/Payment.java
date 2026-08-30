package org.example.trusttrade.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(unique = true)
    private String paymentKey;


    private int amount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.READY;
    //===========================
    @Column(unique = true)
    private String idempotencyKey;

    //실패 이유
    private String failReason;

    @CreatedDate
    private LocalDateTime requestAt;
    @LastModifiedDate
    private LocalDateTime approvedAt;

    public enum Status{
        READY,CONFIRMING, SUCCESS, FAILED
    }

    //paymentKey 할당
    public void assignPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }


    public static Payment create(Order order, String idempotencyKey) {

        if (order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey는 필수입니다.");
        }

        Payment payment = new Payment();
        payment.idempotencyKey = idempotencyKey;

        order.addPayment(payment);

        return payment;
    }

    public void setOrder(Order order) {
        this.order = order;
    }


    public void setFailReason(String failReason){
        this.failReason = failReason;
    }

    //상태 전이 함수
    public void changeStatus(Status newStatus) {

        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                    String.format(
                            "잘못된 Payment 상태 변경입니다. %s -> %s",
                            this.status,
                            newStatus
                    )
            );
        }

        this.status = newStatus;
    }

    private boolean isValidTransition(Status current, Status next) {

        return switch (current) {
            case READY -> next == Status.CONFIRMING;
            case CONFIRMING ->
                    next == Status.SUCCESS ||
                            next == Status.FAILED;
            case SUCCESS, FAILED -> false;
        };
    }

}

