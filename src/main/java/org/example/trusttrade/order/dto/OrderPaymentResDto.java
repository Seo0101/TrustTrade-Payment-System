package org.example.trusttrade.order.dto;

import lombok.*;
import org.example.trusttrade.order.domain.Order;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentResDto {

    private String orderId;
    private int amount;
    private String productName;
    private Order.Status status;

}
