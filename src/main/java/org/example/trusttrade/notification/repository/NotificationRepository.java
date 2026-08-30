package org.example.trusttrade.notification.repository;


import org.example.trusttrade.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId")
    List<Notification> getNotiesByUserId(@Param("userId") UUID userId);

}
