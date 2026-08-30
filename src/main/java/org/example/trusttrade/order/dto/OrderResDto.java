package org.example.trusttrade.order.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.trusttrade.order.domain.Order;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OrderResDto {
    private String orderId;
    private int amount;
    private String productName;
    private UUID sellerId;
    private UUID buyerId;

    public OrderResDto(Order order) {

        this.orderId = order.getId();
        this.amount = order.getAmount();
        this.productName = order.getProductName();
        this.sellerId = order.getSeller().getId();
        this.buyerId = order.getBuyer().getId();
    }
}
