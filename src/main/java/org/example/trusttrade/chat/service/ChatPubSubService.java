package org.example.trusttrade.chat.service;

import org.example.trusttrade.chat.dto.ChatMessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatPubSubService {
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatPubSubService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String roomId, ChatMessageDto message) {
        redisTemplate.convertAndSend("chat:room:" + roomId + ":pub", message);
    }
}