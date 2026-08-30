package org.example.trusttrade.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.trusttrade.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NotiResDto {
    private String content;
    private UUID userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    public NotiResDto(Notification noti) {
        this.content = noti.getContent();
        this.userId = noti.getUser().getId();
        this.createdTime = noti.getCreatedAt();
    }
}
