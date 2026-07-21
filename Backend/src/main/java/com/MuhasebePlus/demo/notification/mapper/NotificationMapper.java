package com.MuhasebePlus.demo.notification.mapper;

import org.springframework.stereotype.Component;

import com.MuhasebePlus.demo.notification.dto.response.NotificationResponseDto;
import com.MuhasebePlus.demo.notification.entity.Notification;

@Component
public class NotificationMapper {

    public NotificationResponseDto toResponseDto(Notification notification) {
        return new NotificationResponseDto(
                notification.getNotificationId(),
                notification.getUser() != null ? notification.getUser().getUserId() : null,
                notification.getRecipientEmail(),
                notification.getNotificationType() != null ? notification.getNotificationType().name() : null,
                notification.getSubject(),
                notification.getMessage(),
                notification.getSentAt(),
                notification.getReadAt(),
                notification.getReadAt() != null,
                notification.isEmailSent(),
                notification.getEmailError(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
