package org.example.trusttrade.chat.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class ChatRoomRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatRoomRedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getOrCreateRoom(String sellerId, String buyerId, String itemId) {
        String roomId = buildRoomId(sellerId, buyerId, itemId);
        String key = "chat:room:" + roomId + ":exists";
        Boolean exists = redisTemplate.hasKey(key);
        if (exists != null && exists) {
            return roomId;
        }
        redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS);
        redisTemplate.opsForSet().add("chat:user:" + sellerId + ":rooms", roomId);
        redisTemplate.opsForSet().add("chat:user:" + buyerId + ":rooms", roomId);
        return roomId;
    }

    public boolean roomExists(String sellerId, String buyerId, String itemId) {
        String roomId = buildRoomId(sellerId, buyerId, itemId);
        String key = "chat:room:" + roomId + ":exists";
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    public Set<Object> getUserRooms(String userId) {
        return redisTemplate.opsForSet().members("chat:user:" + userId + ":rooms");
    }

    public static String buildRoomId(String sellerId, String buyerId, String itemId) {
        return sellerId + ":" + buyerId + ":" + itemId;
    }
}