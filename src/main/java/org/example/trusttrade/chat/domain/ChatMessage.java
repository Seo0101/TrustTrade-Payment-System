package org.example.trusttrade.chat.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;
    private String senderId;
    private String content;
    private Long timestamp;
    private boolean is_read;
}
