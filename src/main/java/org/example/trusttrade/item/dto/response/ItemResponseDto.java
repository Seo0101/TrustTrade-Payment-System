package org.example.trusttrade.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.item.domain.products.Product;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ItemResponseDto {

    private Long itemId;
    private String sellerAccount; // ← 소문자로
    private String name;
    private String itemType;
    private String description;
    private Integer price;        // Product/경매 시작가 공통 슬롯

    public static ItemResponseDto fromProduct(Product p) {
        return ItemResponseDto.builder()
                .itemId(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .sellerAccount(p.getUser().getUserAccount())
                .price(p.getProductPrice())
                .itemType("PRODUCT")
                .build();
    }

    public static ItemResponseDto fromAuction(Auction a) {
        return ItemResponseDto.builder()
                .itemId(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .sellerAccount(a.getUser().getUserAccount())
                .price(a.getStartPrice())   // 시작가를 price로 사용
                .itemType("AUCTION")
                .build();
    }


}