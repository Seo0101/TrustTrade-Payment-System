package org.example.trusttrade.chat.service;

import org.example.trusttrade.chat.domain.ChatMessage;
import org.example.trusttrade.chat.dto.ChatMessageDto;
import org.example.trusttrade.chat.dto.ChatRoomCreateRequest;
import org.example.trusttrade.chat.repository.ChatMessageRedisRepository;
import org.example.trusttrade.chat.repository.ChatMessageRepository;
import org.example.trusttrade.chat.repository.ChatRoomRedisRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final ChatMessageRedisRepository chatMessageRedisRepository;
    private final ChatPubSubService chatPubSubService;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatRoomRedisRepository chatRoomRedisRepository, ChatMessageRedisRepository chatMessageRedisRepository, ChatPubSubService chatPubSubService, ChatMessageRepository chatMessageRepository) {
        this.chatRoomRedisRepository = chatRoomRedisRepository;
        this.chatMessageRedisRepository = chatMessageRedisRepository;
        this.chatPubSubService = chatPubSubService;
        this.chatMessageRepository = chatMessageRepository;
    }

    public String createOrGetRoom(ChatRoomCreateRequest req) {
        return chatRoomRedisRepository.getOrCreateRoom(req.getSellerId(), req.getBuyerId(), req.getItemId());
    }

    public Set<Object> getUserRooms(String userId) {
        return chatRoomRedisRepository.getUserRooms(userId);
    }

    public void sendMessage(String roomId, ChatMessageDto message) {
        chatMessageRedisRepository.saveMessage(roomId, message);
        chatPubSubService.publish(roomId, message);

        ChatMessage entity = ChatMessage.builder()
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .is_read(message.isRead())
                .build();
        chatMessageRepository.save(entity);
    }

    public List<ChatMessageDto> getMessages(String roomId) {
        return chatMessageRedisRepository.getMessages(roomId);
    }

    public void markAllRead(String roomId, String userId) {
        chatMessageRedisRepository.markAllRead(roomId, userId);
    }
}