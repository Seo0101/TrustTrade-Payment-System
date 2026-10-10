package org.example.trusttrade.item.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ReserveRequest {

    @NotNull
    private UUID sellerId;
    @NotNull
    private UUID buyerId;


    public UUID getBuyerId() {
        return buyerId;
    }

    public UUID getSellerId(){return sellerId;}
}
