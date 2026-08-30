package org.example.trusttrade.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomCreateRequest {
    private String sellerId;
    private String buyerId;
    private String itemId;
}