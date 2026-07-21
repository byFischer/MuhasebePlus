package com.MuhasebePlus.demo.cheque.service;

import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequeReminderSchedulerTest {

    @Mock private ChequeRepository chequeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks private ChequeReminderScheduler scheduler;

    @Test
    void sendDueReminders_whenAdminsAndDueCheques_existSendsNotifications() {
        Company company = company(1L);
        User admin = admin(10L, company, "admin@test.com");
        User otherCompanyAdmin = admin(11L, company(2L), "other@test.com");
        LocalDate today = LocalDate.now();
        Cheque dueInSevenDays = cheque(company, "CHK-007", today.plusDays(7), ChequeKind.CHEQUE);
        Cheque dueTomorrow = cheque(company, "SNT-001", today.plusDays(1), ChequeKind.PROMISSORY_NOTE);
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin, otherCompanyAdmin));
        when(chequeRepository.findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                1L, ChequeStatus.IN_PORTFOLIO, today.plusDays(7), today.plusDays(7)))
                .thenReturn(List.of(dueInSevenDays));
        when(chequeRepository.findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                1L, ChequeStatus.IN_PORTFOLIO, today.plusDays(3), today.plusDays(3)))
                .thenReturn(List.of());
        when(chequeRepository.findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                1L, ChequeStatus.IN_PORTFOLIO, today.plusDays(1), today.plusDays(1)))
                .thenReturn(List.of(dueTomorrow));

        scheduler.sendDueReminders();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getRecipientEmail)
                .containsExactly("admin@test.com", "admin@test.com");
        assertThat(captor.getAllValues()).extracting(Notification::getNotificationType)
                .containsExactly(NotificationType.warning, NotificationType.error);
        assertThat(captor.getAllValues().get(0).getSubject()).contains("7");
        assertThat(captor.getAllValues().get(1).getSubject()).contains("Yar");
        assertThat(captor.getAllValues().get(0).getMessage()).contains("CHK-007");
        assertThat(captor.getAllValues().get(1).getMessage()).contains("SNT-001");
    }

    @Test
    void sendDueReminders_whenNoAdminsForCompany_skipsChequeLookup() {
        Company company = company(1L);
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin(20L, company(2L), "other@test.com")));

        scheduler.sendDueReminders();

        verify(chequeRepository, never())
                .findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                        org.mockito.Mockito.anyLong(),
                        org.mockito.Mockito.any(),
                        org.mockito.Mockito.any(),
                        org.mockito.Mockito.any());
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setCompanyId(id);
        company.setCompanyName("Test A.S.");
        return company;
    }

    private User admin(Long id, Company company, String email) {
        User user = new User();
        user.setUserId(id);
        user.setCompany(company);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        return user;
    }

    private Cheque cheque(Company company, String number, LocalDate dueDate, ChequeKind kind) {
        return Cheque.builder()
                .chequeId(1L)
                .company(company)
                .chequeNumber(number)
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(kind)
                .amount(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .issueDate(LocalDate.now())
                .dueDate(dueDate)
                .status(ChequeStatus.IN_PORTFOLIO)
                .build();
    }
}
