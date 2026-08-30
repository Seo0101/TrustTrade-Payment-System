package org.example.trusttrade.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.notification.domain.Notification;
import org.example.trusttrade.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    //알림 생성
    public Notification createNotification(String content, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Notification noti = Notification.create(user, content);

        notificationRepository.save(noti);
        return noti;
    }

    //알림 목록 조회
    public List<Notification> getnotiesByUserId(UUID userId) {
        List<Notification> noties = notificationRepository.getNotiesByUserId(userId);

        return noties;
    }

    //읽음 처리
    public void isRead(Long notiId){
        Optional<Notification> noti = notificationRepository.findById(notiId);

        if(noti.isPresent()){
            noti.get().isRead();
        }else{
            throw new IllegalArgumentException("해당 알림 객체가 존재하지 않습니다 : " + notiId);
        }
    }

}
