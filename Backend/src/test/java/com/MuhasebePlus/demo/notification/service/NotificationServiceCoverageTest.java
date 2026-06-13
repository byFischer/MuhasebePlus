package com.MuhasebePlus.demo.notification.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.notification.dto.request.NotificationRequestDto;
import com.MuhasebePlus.demo.notification.dto.response.NotificationResponseDto;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.mapper.NotificationMapper;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceCoverageTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private Environment environment;
    @Mock private SystemLogService systemLogService;
    @Mock private JavaMailSender mailSender;

    private NotificationService service;
    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                notificationRepository,
                userRepository,
                companyRepository,
                companyContext,
                new NotificationMapper(),
                mailSenderProvider,
                environment,
                systemLogService
        );
        company = new Company();
        company.setCompanyId(1L);
        user = userWithCompany(7L, company);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
    }

    @Test
    void createNotification_savesNotificationAndReturnsMappedDto() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setNotificationId(11L);
            return notification;
        });

        NotificationResponseDto result = service.createNotification(
                new NotificationRequestDto(7L, NotificationType.info, "Subject", "Message", false));

        assertThat(result.notificationId()).isEqualTo(11L);
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.recipientEmail()).isEqualTo("user7@example.com");
        assertThat(result.notificationType()).isEqualTo("info");
        assertThat(result.emailSent()).isFalse();
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void createForUser_whenMailHostBlank_skipsEmailAndClearsEmailError() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(environment.getProperty("spring.mail.host")).thenReturn(" ");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.createForUser(7L, NotificationType.warning, "Subject", "Message", true);

        assertThat(result.isEmailSent()).isFalse();
        assertThat(result.getEmailError()).isNull();
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void createForUser_whenMailSenderMissing_recordsAvailabilityError() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.local");
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.createForUser(7L, NotificationType.warning, "Subject", "Message", true);

        assertThat(result.isEmailSent()).isFalse();
        assertThat(result.getEmailError()).isEqualTo("Mail sender is not available");
    }

    @Test
    void createForUser_whenMailSenderConfigured_sendsEmailWithOptionalFromAddress() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.local");
        when(environment.getProperty("spring.mail.username")).thenReturn("noreply@test.com");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.createForUser(7L, NotificationType.info, "Subject", "Message", true);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("noreply@test.com");
        assertThat(captor.getValue().getTo()).containsExactly("user7@example.com");
        assertThat(result.isEmailSent()).isTrue();
        assertThat(result.getEmailError()).isNull();
    }

    @Test
    void createForUser_whenMailSenderThrows_truncatesLongEmailError() {
        String longMessage = "x".repeat(1200);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.local");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException(longMessage)).when(mailSender).send(any(SimpleMailMessage.class));

        Notification result = service.createForUser(7L, NotificationType.error, "Subject", "Message", true);

        assertThat(result.isEmailSent()).isFalse();
        assertThat(result.getEmailError()).hasSize(1000);
    }

    @Test
    void createForUser_whenUserMissing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createForUser(99L, NotificationType.info, "Subject", "Message", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getCurrentUserNotifications_usesUnreadOrAllRepositoryQuery() {
        Notification unread = notification(1L, null);
        Notification read = notification(2L, LocalDateTime.of(2026, 1, 1, 10, 0));
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(notificationRepository.findByUserUserIdAndCompanyCompanyIdAndReadAtIsNullOrderBySentAtDesc(7L, 1L))
                .thenReturn(List.of(unread));
        when(notificationRepository.findByUserUserIdAndCompanyCompanyIdOrderBySentAtDesc(7L, 1L))
                .thenReturn(List.of(read, unread));

        assertThat(service.getCurrentUserNotifications(true)).hasSize(1);
        assertThat(service.getCurrentUserNotifications(false)).hasSize(2);
    }

    @Test
    void markAllAsRead_setsReadAtForEveryUnreadNotification() {
        Notification first = notification(1L, null);
        Notification second = notification(2L, null);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(notificationRepository.findByUserUserIdAndCompanyCompanyIdAndReadAtIsNullOrderBySentAtDesc(7L, 1L))
                .thenReturn(List.of(first, second));
        when(notificationRepository.saveAll(List.of(first, second))).thenReturn(List.of(first, second));

        List<NotificationResponseDto> result = service.markAllAsRead();

        assertThat(result).hasSize(2);
        assertThat(first.getReadAt()).isNotNull();
        assertThat(second.getReadAt()).isNotNull();
        assertThat(result).allSatisfy(dto -> assertThat(dto.read()).isTrue());
    }

    @Test
    void markAsRead_whenAlreadyRead_keepsExistingReadAt() {
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        Notification notification = notification(11L, readAt);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(notificationRepository.findByNotificationIdAndUserUserIdAndCompanyCompanyId(11L, 7L, 1L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponseDto result = service.markAsRead(11L);

        assertThat(result.readAt()).isEqualTo(readAt);
        assertThat(notification.getReadAt()).isEqualTo(readAt);
    }

    private Notification notification(Long id, LocalDateTime readAt) {
        Notification notification = new Notification();
        notification.setNotificationId(id);
        notification.setCompany(company);
        notification.setUser(user);
        notification.setRecipientEmail(user.getEmail());
        notification.setNotificationType(NotificationType.info);
        notification.setSubject("Subject " + id);
        notification.setMessage("Message " + id);
        notification.setSentAt(LocalDateTime.of(2026, 1, 1, 9, 0).plusHours(id));
        notification.setReadAt(readAt);
        return notification;
    }

    private User userWithCompany(Long userId, Company company) {
        User user = new User();
        user.setUserId(userId);
        user.setCompany(company);
        user.setEmail("user" + userId + "@example.com");
        return user;
    }
}
