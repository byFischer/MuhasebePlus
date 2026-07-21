package com.MuhasebePlus.demo.notification.controller;

import com.MuhasebePlus.demo.notification.dto.request.NotificationRequestDto;
import com.MuhasebePlus.demo.notification.dto.response.NotificationResponseDto;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerUnitTest {

    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void endpoints_delegateToNotificationService() {
        NotificationRequestDto request = new NotificationRequestDto(
                7L, NotificationType.info, "Subject", "Message", true);
        NotificationResponseDto response = response(11L);
        when(notificationService.getCurrentUserNotifications(true)).thenReturn(List.of(response));
        when(notificationService.getCurrentUserUnreadCount()).thenReturn(3L);
        when(notificationService.createNotification(request)).thenReturn(response);
        when(notificationService.markAsRead(11L)).thenReturn(response);
        when(notificationService.markAllAsRead()).thenReturn(List.of(response));

        assertThat(controller.getNotifications(true).getBody()).containsExactly(response);
        assertThat(controller.getUnreadCount().getBody()).containsEntry("count", 3L);
        assertThat(controller.createNotification(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.createNotification(request).getBody()).isSameAs(response);
        assertThat(controller.markAsRead(11L).getBody()).isSameAs(response);
        assertThat(controller.markAllAsRead().getBody()).containsExactly(response);
        verify(notificationService).markAsRead(11L);
    }

    private NotificationResponseDto response(Long id) {
        return new NotificationResponseDto(
                id,
                7L,
                "user7@example.com",
                "info",
                "Subject",
                "Message",
                LocalDateTime.of(2026, 1, 1, 9, 0),
                null,
                false,
                false,
                null,
                null,
                null
        );
    }
}
