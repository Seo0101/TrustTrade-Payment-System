package org.example.trusttrade.item.domain.products;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.Item;
import org.example.trusttrade.item.dto.request.BasicItemDto;

import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.ProductStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder
@DiscriminatorValue("PRODUCT")
@Table(name = "product")
public class Product extends Item {

    @Column(name="product_price")
    private Integer productPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status",nullable = false)
    private ProductStatus status;

    //구매자 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_buyer_id")
    private User reservedBuyer;

    //중복 예약 처리 방지
    @Transactional
    public boolean isIdempotentRequest(UUID buyerId) {
        return this.status == ProductStatus.RESERVED &&
                this.reservedBuyer != null &&
                this.reservedBuyer.getId().equals(buyerId);
    }

    //상품 판매 처리
    public void resale(){
        if(this.status != ProductStatus.RESERVED){
            throw new ProductStatusException("상품 판매 처리가 불가능합니다. ");
        }
        this.status = ProductStatus.SALE;
        this.reservedBuyer = null;
    }

    //상품 완료 처리
    public void setCompleted(){
        if(this.status != ProductStatus.RESERVED){
            throw new ProductStatusException("상품 판매 완료 처리가 불가능합니다. ");
        }
        this.status = ProductStatus.COMPLETED;
    }

    //상품 상태 예약 변경
    public void reserve(User buyer) {
        if(this.status != ProductStatus.SALE){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        this.reservedBuyer = buyer;
        this.status = ProductStatus.RESERVED;
    }

    //상품 정보 및 구매자 정보 검증
    public void validateOrderable(UUID buyerId) {

        // 1. 자기 구매 방지
        if (this.getUser() != null && this.getUser().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.SELF_PURCHASE_NOT_ALLOWED);
        }
        //2. 판매 완료 상품
        if (this.status == ProductStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        //3. 예약 상품 -> 구매자 검증, 재결제인 경우 reserved임.
        if (this.status == ProductStatus.RESERVED) {
            if (this.reservedBuyer == null ||
                    !this.reservedBuyer.getId().equals(buyerId)) {
                throw new BusinessException(ErrorCode.NOT_RESERVED_BUYER);
            }
        }
    }

    public static Product fromDto(BasicItemDto dto, User seller) {
        // 1) ProductLocation  생성
        ProductLocation loc = ProductLocation.fromDto(dto);

        // 2) Product 객체 생성
        return Product.builder()
                // Item 필드
                .user(seller)
                .name(dto.getTitle())
                .description(dto.getDescription())
                .productLocation(loc)
                .createdTime(LocalDateTime.now())

                // Product 필드
                .productPrice(dto.getPrice())
                .status(ProductStatus.SALE)
                .build();


    }

}