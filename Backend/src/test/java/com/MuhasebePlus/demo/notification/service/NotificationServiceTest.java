package com.MuhasebePlus.demo.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.notification.dto.response.NotificationResponseDto;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.mapper.NotificationMapper;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private CompanyContext companyContext;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        companyContext = mock(CompanyContext.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        Environment environment = mock(Environment.class);
        SystemLogService systemLogService = mock(SystemLogService.class);

        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                companyRepository,
                companyContext,
                new NotificationMapper(),
                mailSenderProvider,
                environment,
                systemLogService
        );
    }

    @Test
    void createForUserRejectsRecipientFromAnotherCompany() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(userWithCompany(10L, 2L)));

        assertThrows(RuntimeException.class, () ->
                notificationService.createForUser(10L, NotificationType.info, "Subject", "Message", false));
    }

    @Test
    void getCurrentUserUnreadCountUsesCurrentUserAndCompany() {
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyContext.getCurrentCompanyId()).thenReturn(3L);
        when(notificationRepository.countByUserUserIdAndCompanyCompanyIdAndReadAtIsNull(7L, 3L)).thenReturn(4L);

        assertEquals(4L, notificationService.getCurrentUserUnreadCount());
    }

    @Test
    void markAsReadRejectsNotificationOutsideCurrentUserScope() {
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyContext.getCurrentCompanyId()).thenReturn(3L);
        when(notificationRepository.findByNotificationIdAndUserUserIdAndCompanyCompanyId(99L, 7L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(99L));
    }

    @Test
    void markAsReadSetsReadAtForCurrentUserNotification() {
        Notification notification = new Notification();
        notification.setNotificationId(11L);
        notification.setUser(userWithCompany(7L, 3L));
        notification.setNotificationType(NotificationType.warning);
        notification.setSubject("Subject");
        notification.setMessage("Message");

        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyContext.getCurrentCompanyId()).thenReturn(3L);
        when(notificationRepository.findByNotificationIdAndUserUserIdAndCompanyCompanyId(11L, 7L, 3L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponseDto response = notificationService.markAsRead(11L);

        assertEquals(true, response.read());
        verify(notificationRepository).save(notification);
    }

    private User userWithCompany(Long userId, Long companyId) {
        Company company = new Company();
        company.setCompanyId(companyId);

        User user = new User();
        user.setUserId(userId);
        user.setCompany(company);
        user.setEmail("user" + userId + "@example.com");
        return user;
    }
}
