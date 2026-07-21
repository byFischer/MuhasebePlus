package com.MuhasebePlus.demo.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.notification.dto.request.NotificationRequestDto;
import com.MuhasebePlus.demo.notification.dto.response.NotificationResponseDto;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.mapper.NotificationMapper;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_EMAIL_ERROR_LENGTH = 1000;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;
    private final NotificationMapper notificationMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;
    private final SystemLogService systemLogService;

    public NotificationResponseDto createNotification(NotificationRequestDto dto) {
        Notification saved = createForUser(
                dto.recipientUserId(),
                dto.notificationType(),
                dto.subject(),
                dto.message(),
                dto.sendEmail()
        );
        return notificationMapper.toResponseDto(saved);
    }

    public Notification createForUser(
            Long recipientUserId,
            NotificationType type,
            String subject,
            String message,
            boolean sendEmail
    ) {
        Long companyId = companyContext.getCurrentCompanyId();
        User recipient = findCompanyUser(recipientUserId, companyId);

        Notification notification = new Notification();
        notification.setCompany(companyRepository.getReferenceById(companyId));
        notification.setUser(recipient);
        notification.setNotificationType(type);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setSentAt(LocalDateTime.now());
        notification.setRecipientEmail(recipient.getEmail());

        Notification saved = notificationRepository.save(notification);

        if (sendEmail) {
            sendEmailIfConfigured(saved);
            saved = notificationRepository.save(saved);
        }

        systemLogService.log(LogLevel.INFO, "Notification created for user id=" + recipientUserId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getCurrentUserNotifications(boolean unreadOnly) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserUserIdAndCompanyCompanyIdAndReadAtIsNullOrderBySentAtDesc(userId, companyId)
                : notificationRepository.findByUserUserIdAndCompanyCompanyIdOrderBySentAtDesc(userId, companyId);

        return notifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getCurrentUserUnreadCount() {
        return notificationRepository.countByUserUserIdAndCompanyCompanyIdAndReadAtIsNull(
                companyContext.getCurrentUserId(),
                companyContext.getCurrentCompanyId()
        );
    }

    public NotificationResponseDto markAsRead(Long notificationId) {
        Notification notification = findCurrentUserNotification(notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return notificationMapper.toResponseDto(notificationRepository.save(notification));
    }

    public List<NotificationResponseDto> markAllAsRead() {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<Notification> unreadNotifications =
                notificationRepository.findByUserUserIdAndCompanyCompanyIdAndReadAtIsNullOrderBySentAtDesc(userId, companyId);

        unreadNotifications.forEach(notification -> notification.setReadAt(now));
        return notificationRepository.saveAll(unreadNotifications)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    private User findCompanyUser(Long userId, Long companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
            throw new RuntimeException("Recipient user is not in your company");
        }

        return user;
    }

    private Notification findCurrentUserNotification(Long notificationId) {
        return notificationRepository.findByNotificationIdAndUserUserIdAndCompanyCompanyId(
                        notificationId,
                        companyContext.getCurrentUserId(),
                        companyContext.getCurrentCompanyId()
                )
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
    }

    private void sendEmailIfConfigured(Notification notification) {
        String mailHost = environment.getProperty("spring.mail.host");
        if (mailHost == null || mailHost.isBlank()) {
            notification.setEmailSent(false);
            notification.setEmailError(null);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            notification.setEmailSent(false);
            notification.setEmailError("Mail sender is not available");
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            String from = environment.getProperty("spring.mail.username");
            if (from != null && !from.isBlank()) {
                mailMessage.setFrom(from);
            }
            mailMessage.setTo(notification.getRecipientEmail());
            mailMessage.setSubject(notification.getSubject());
            mailMessage.setText(notification.getMessage());

            mailSender.send(mailMessage);
            notification.setEmailSent(true);
            notification.setEmailError(null);
        } catch (RuntimeException ex) {
            notification.setEmailSent(false);
            notification.setEmailError(truncate(ex.getMessage()));
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "Mail could not be sent";
        }
        return value.length() <= MAX_EMAIL_ERROR_LENGTH ? value : value.substring(0, MAX_EMAIL_ERROR_LENGTH);
    }
}
