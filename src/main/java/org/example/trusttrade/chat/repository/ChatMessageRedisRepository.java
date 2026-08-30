package org.example.trusttrade.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trusttrade.chat.dto.ChatMessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ChatMessageRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatMessageRedisRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveMessage(String roomId, ChatMessageDto message) {
        redisTemplate.opsForList().rightPush("chat:room:" + roomId + ":messages", message);
    }

    public List<ChatMessageDto> getMessages(String roomId) {
        List<Object> messages = redisTemplate.opsForList().range("chat:room:" + roomId + ":messages", 0, -1);
        List<ChatMessageDto> result = new ArrayList<>();
        if (messages != null) {
            for (Object obj : messages) {
                if (obj instanceof ChatMessageDto) {
                    result.add((ChatMessageDto) obj);
                } else {
                    result.add(objectMapper.convertValue(obj, ChatMessageDto.class));
                }
            }
        }
        return result;
    }

    public void markAllRead(String roomId, String userId) {
        List<Object> messages = redisTemplate.opsForList().range("chat:room:" + roomId + ":messages", 0, -1);
        if (messages == null) return;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessageDto message = objectMapper.convertValue(messages.get(i), ChatMessageDto.class);
            if (!message.getSenderId().equals(userId)) {
                message.setRead(true);
                redisTemplate.opsForList().set("chat:room:" + roomId + ":messages", i, message);
            }
        }
    }
}
