package org.example.trusttrade.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.trusttrade.login.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String content;

    @CreatedDate
    private LocalDateTime createdAt;

    private boolean isRead = false;

    //알림 생성
    public static Notification create(User user, String content) {
        Notification notification = new Notification();
        notification.user = user;
        notification.content = content;

        return notification;
    }

    //읽음 상태 변경
    public void isRead(){
        this.isRead = true;
    }
}
