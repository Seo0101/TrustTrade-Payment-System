package org.example.trusttrade.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.notification.domain.Notification;
import org.example.trusttrade.notification.dto.NotiResDto;
import org.example.trusttrade.notification.dto.NotificationForm;
import org.example.trusttrade.notification.repository.NotificationRepository;
import org.example.trusttrade.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;


    //알림 생성
    @PostMapping("/new")
    public ResponseEntity<?> createNotification(@RequestBody NotificationForm request) {
        Notification noti = notificationService.createNotification(request.getContent(), request.getUserId());

        NotiResDto resDto = new NotiResDto(noti);
        return ResponseEntity.ok(resDto);
    }

    //알림 조회
    @GetMapping("/{userId}/list")
    public ResponseEntity<?> getNotifications(@PathVariable("userId") UUID userId) {

        List<Notification> noties = notificationService.getnotiesByUserId(userId);

        //객체가 fetch로 설정되서 프록시 상태 객체라 json으로 직렬화가 안되는 문제 발생
        //dto로 값 리턴
        List<NotiResDto> response = noties.stream()
                .map(NotiResDto::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    //알림 읽음 상태로 변경
    @PostMapping("/{notiId}/read")
    public ResponseEntity<?> readNotification(@PathVariable("notiId") Long notiId) {

        try{
            notificationService.isRead(notiId);
            return ResponseEntity.ok("알림 읽음 상태 변경 완료");
        }catch (Exception notFoundNoti) {
            return ResponseEntity.badRequest().body(notFoundNoti.getMessage());
        }
    }



}
