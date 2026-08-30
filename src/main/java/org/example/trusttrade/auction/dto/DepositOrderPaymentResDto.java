package org.example.trusttrade.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepositOrderPaymentResDto {

    private String orderId;
    private int amount;
    private String productName;
}
