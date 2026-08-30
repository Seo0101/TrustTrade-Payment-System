package org.example.trusttrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.trusttrade.item.domain.products.Product;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductReserveResponse {
    private Long product;
    private UUID sellerId;
    private UUID buyerId;
}
