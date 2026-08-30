package org.example.trusttrade.chat.controller;

import org.example.trusttrade.chat.dto.ChatRoomCreateRequest;
import org.example.trusttrade.chat.service.ChatService;
import org.example.trusttrade.chat.dto.ChatMessageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/room")
    public ResponseEntity<String> createOrGetRoom(@RequestBody ChatRoomCreateRequest req) {
        String roomId = chatService.createOrGetRoom(req);
        return ResponseEntity.ok(roomId);
    }

    @GetMapping("/rooms/{userId}")
    public ResponseEntity<Set<Object>> getUserRooms(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUserRooms(userId));
    }

    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessageDto message) {
        chatService.sendMessage(message.getRoomId(), message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/messages/{roomId}")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }

    @PostMapping("/read/{roomId}/{userId}")
    public ResponseEntity<Void> markAllRead(@PathVariable String roomId, @PathVariable String userId) {
        chatService.markAllRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
