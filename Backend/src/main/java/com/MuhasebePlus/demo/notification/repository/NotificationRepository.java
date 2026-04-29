package com.MuhasebePlus.demo.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUserIdAndCompanyCompanyIdOrderBySentAtDesc(Long userId, Long companyId);

    List<Notification> findByUserUserIdAndCompanyCompanyIdAndReadAtIsNullOrderBySentAtDesc(Long userId, Long companyId);

    long countByUserUserIdAndCompanyCompanyIdAndReadAtIsNull(Long userId, Long companyId);

    Optional<Notification> findByNotificationIdAndUserUserIdAndCompanyCompanyId(
            Long notificationId,
            Long userId,
            Long companyId
    );
}
