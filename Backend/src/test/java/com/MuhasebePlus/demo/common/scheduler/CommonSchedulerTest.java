package com.MuhasebePlus.demo.common.scheduler;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonSchedulerTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HardDeletable hardDeletable;
    @Mock private HardDeletable emptyHardDeletable;

    @Test
    void cleanupScheduler_callsEveryHardDeletableWithThirtyDayCutoff() {
        when(hardDeletable.hardDeleteExpired(any(LocalDateTime.class))).thenReturn(3);
        when(emptyHardDeletable.hardDeleteExpired(any(LocalDateTime.class))).thenReturn(0);
        CleanupScheduler scheduler = new CleanupScheduler(List.of(hardDeletable, emptyHardDeletable));

        scheduler.runCleanup();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(hardDeletable).hardDeleteExpired(captor.capture());
        verify(emptyHardDeletable).hardDeleteExpired(any(LocalDateTime.class));
        assertThat(captor.getValue()).isBefore(LocalDateTime.now().minusDays(29));
        assertThat(captor.getValue()).isAfter(LocalDateTime.now().minusDays(31));
    }

    @Test
    void overdueScheduler_marksPendingAndPartlyPaidInvoicesAsOverduePerCompany() {
        Company firstCompany = company(1L);
        Company duplicateCompany = company(1L);
        Company secondCompany = company(2L);
        Invoice pending = invoice(PaymentStatus.pending);
        Invoice partlyPaid = invoice(PaymentStatus.partially_paid);
        when(companyRepository.findAll()).thenReturn(List.of(firstCompany, duplicateCompany, secondCompany));
        when(invoiceRepository.findByDueDateBeforeAndPaymentStatusInAndCompanyCompanyIdAndIsDeletedFalse(
                eq(LocalDate.now()), eq(List.of(PaymentStatus.pending, PaymentStatus.partially_paid)), eq(1L)))
                .thenReturn(List.of(pending));
        when(invoiceRepository.findByDueDateBeforeAndPaymentStatusInAndCompanyCompanyIdAndIsDeletedFalse(
                eq(LocalDate.now()), eq(List.of(PaymentStatus.pending, PaymentStatus.partially_paid)), eq(2L)))
                .thenReturn(List.of(partlyPaid));
        OverdueScheduler scheduler = new OverdueScheduler(invoiceRepository, companyRepository);

        scheduler.markOverdueInvoices();

        assertThat(pending.getPaymentStatus()).isEqualTo(PaymentStatus.overdue);
        assertThat(partlyPaid.getPaymentStatus()).isEqualTo(PaymentStatus.overdue);
        verify(invoiceRepository).save(pending);
        verify(invoiceRepository).save(partlyPaid);
    }

    @Test
    void overdueScheduler_whenNoInvoicesDue_doesNotSave() {
        when(companyRepository.findAll()).thenReturn(List.of(company(1L)));
        when(invoiceRepository.findByDueDateBeforeAndPaymentStatusInAndCompanyCompanyIdAndIsDeletedFalse(
                eq(LocalDate.now()), eq(List.of(PaymentStatus.pending, PaymentStatus.partially_paid)), eq(1L)))
                .thenReturn(List.of());
        OverdueScheduler scheduler = new OverdueScheduler(invoiceRepository, companyRepository);

        scheduler.markOverdueInvoices();

        verify(invoiceRepository, never()).save(any());
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setCompanyId(id);
        company.setCompanyName("Test A.S.");
        return company;
    }

    private Invoice invoice(PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setPaymentStatus(status);
        return invoice;
    }
}
