package com.MuhasebePlus.demo.cheque.service;

import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChequeReminderScheduler {

    private static final int[] REMINDER_DAYS = {7, 3, 1};

    private final ChequeRepository chequeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CompanyRepository companyRepository;

    // OverdueScheduler 01:00'de çalışır, bu 03:00'de — çakışmaz
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void sendDueReminders() {
        LocalDate today = LocalDate.now();
        int notified = 0;

        // Şirket bazlı tarama
        List<Long> companyIds = companyRepository.findAll().stream()
                .map(c -> c.getCompanyId()).toList();

        for (Long companyId : companyIds) {
            List<User> admins = userRepository.findByRole(UserRole.ADMIN).stream()
                    .filter(u -> u.getCompany() != null && companyId.equals(u.getCompany().getCompanyId()))
                    .toList();

            if (admins.isEmpty()) continue;

            for (int days : REMINDER_DAYS) {
                LocalDate targetDate = today.plusDays(days);
                List<Cheque> dueCheques = chequeRepository
                        .findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                                companyId, ChequeStatus.IN_PORTFOLIO, targetDate, targetDate);

                for (Cheque cheque : dueCheques) {
                    String subject = days == 1
                            ? "Yarın vadeli çek/senet"
                            : days + " gün sonra vadeli çek/senet";
                    String message = String.format(
                            "%s no'lu %s vadesi %s tarihinde dolmaktadır. Tutar: %.2f %s",
                            cheque.getChequeNumber(),
                            cheque.getChequeKind().name().toLowerCase(),
                            cheque.getDueDate(),
                            cheque.getAmount(),
                            cheque.getCurrency());

                    NotificationType type = days == 1
                            ? NotificationType.error : NotificationType.warning;

                    for (User admin : admins) {
                        Notification notification = new Notification();
                        notification.setCompany(cheque.getCompany());
                        notification.setUser(admin);
                        notification.setNotificationType(type);
                        notification.setSubject(subject);
                        notification.setMessage(message);
                        notification.setSentAt(LocalDateTime.now());
                        notification.setRecipientEmail(admin.getEmail());
                        notificationRepository.save(notification);
                        notified++;
                    }
                }
            }
        }

        if (notified > 0) {
            log.info("Çek/senet vade hatırlatması: {} bildirim gönderildi", notified);
        }
    }
}
