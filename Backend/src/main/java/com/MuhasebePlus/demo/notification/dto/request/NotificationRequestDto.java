package com.MuhasebePlus.demo.notification.dto.request;

import com.MuhasebePlus.demo.notification.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequestDto(
        @NotNull(message = "Recipient user is required")
        Long recipientUserId,

        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject must be at most 255 characters")
        String subject,

        @NotBlank(message = "Message is required")
        String message,

        boolean sendEmail
) {
}
