package com.MuhasebePlus.demo.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long notificationId,
        Long userId,
        String recipientEmail,
        String notificationType,
        String subject,
        String message,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        boolean read,
        boolean emailSent,
        String emailError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
