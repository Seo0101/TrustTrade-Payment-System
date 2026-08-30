package org.example.trusttrade.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDto {
    private Long item_id;            // 상품 ID
    private String sellerAccount; // 판매자 이름
    private String title;       // 상품 이름
    private int price;          // 가격
    private double latitude;    // 위도
    private double longitude;   // 경도
}