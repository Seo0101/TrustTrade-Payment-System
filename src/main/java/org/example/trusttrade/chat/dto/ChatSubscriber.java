package org.example.trusttrade.chat.dto;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ChatSubscriber implements MessageListener {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            ChatMessageDto msg = objectMapper.readValue(json, ChatMessageDto.class);
            // WebSocket, 알림 등으로 후처리
        } catch (Exception ignored) {
        }
    }
}