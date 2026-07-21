package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.LateFeeConfig;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.repository.LateFeeConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LateFeeServiceTest {

    @Mock private LateFeeConfigRepository lateFeeConfigRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private LateFeeService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long INVOICE_ID = 5L;

    // ── calculateLateFee: zero / rejection paths ──────────────────────────────

    @Test
    void calculateLateFee_whenInvoiceNotFound_throwsRuntimeException() {
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateLateFee(INVOICE_ID, COMPANY_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invoice not found");
    }

    @Test
    void calculateLateFee_whenInvoicePaid_returnsZero() {
        Invoice invoice = invoiceStub(PaymentStatus.paid, LocalDate.now().minusDays(60), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(lateFeeConfigRepository, never()).findByCompanyCompanyIdAndIsActiveTrue(any());
    }

    @Test
    void calculateLateFee_whenDueDateIsNull_returnsZero() {
        Invoice invoice = invoiceStub(PaymentStatus.pending, null, "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(lateFeeConfigRepository, never()).findByCompanyCompanyIdAndIsActiveTrue(any());
    }

    @Test
    void calculateLateFee_whenNotYetDue_returnsZero() {
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().plusDays(5), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(lateFeeConfigRepository, never()).findByCompanyCompanyIdAndIsActiveTrue(any());
    }

    @Test
    void calculateLateFee_whenNoActiveConfig_returnsZero() {
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(60), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.empty());

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateLateFee_whenWithinGracePeriod_returnsZero() {
        // daysOverdue = 7, grace = 7 → 7 <= 7, ücret yok
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(7), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.00", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── calculateLateFee: computation ─────────────────────────────────────────

    @Test
    void calculateLateFee_whenTenDaysPastGrace_chargesOneFullMonth() {
        // daysOverdue = 17, grace = 7 → overdue 10 gün → 1 aya yuvarlanır
        // 1000.00 * 2.00 * 1 / 100 = 20.00
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(17), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.00", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void calculateLateFee_whenExactlyThirtyOverdueDays_chargesExactlyOneMonth() {
        // daysOverdue = 37, grace = 7 → overdue 30 gün → tam 1 ay (yukarı yuvarlama yok)
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(37), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.00", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void calculateLateFee_whenPartialSecondMonth_roundsUpToTwoMonths() {
        // daysOverdue = 38, grace = 7 → overdue 31 gün → 2 aya yuvarlanır
        // 1000.00 * 2.00 * 2 / 100 = 40.00
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(38), "1000.00");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.00", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void calculateLateFee_roundsHalfUpToTwoDecimals() {
        // 1234.56 * 2.50 * 1 / 100 = 30.864 → HALF_UP → 30.86
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(17), "1234.56");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.50", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(new BigDecimal("30.86"));
    }

    @Test
    void calculateLateFee_whenTotalAmountNull_returnsZero() {
        Invoice invoice = invoiceStub(PaymentStatus.pending, LocalDate.now().minusDays(17), null);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(configStub("2.00", 7)));

        BigDecimal result = service.calculateLateFee(INVOICE_ID, COMPANY_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getConfig ─────────────────────────────────────────────────────────────

    @Test
    void getConfig_whenNoneActive_returnsDefaultTwoPercentSevenDays() {
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.empty());

        LateFeeConfig result = service.getConfig(COMPANY_ID);

        assertThat(result.getMonthlyRate()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(result.getGracePeriodDays()).isEqualTo(7);
        assertThat(result.isActive()).isTrue();
        verify(lateFeeConfigRepository, never()).save(any());
    }

    @Test
    void getConfig_whenActiveExists_returnsStoredConfig() {
        LateFeeConfig config = configStub("3.50", 10);
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(config));

        LateFeeConfig result = service.getConfig(COMPANY_ID);

        assertThat(result).isSameAs(config);
        assertThat(result.getMonthlyRate()).isEqualByComparingTo(new BigDecimal("3.50"));
        assertThat(result.getGracePeriodDays()).isEqualTo(10);
    }

    // ── saveConfig ────────────────────────────────────────────────────────────

    @Test
    void saveConfig_whenNoneExists_createsNewConfigWithCompanyReference() {
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.empty());
        when(lateFeeConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LateFeeConfig result = service.saveConfig(COMPANY_ID, new BigDecimal("1.75"), 5);

        ArgumentCaptor<LateFeeConfig> captor = ArgumentCaptor.forClass(LateFeeConfig.class);
        verify(lateFeeConfigRepository).save(captor.capture());
        LateFeeConfig saved = captor.getValue();
        assertThat(saved.getCompany().getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(saved.getMonthlyRate()).isEqualByComparingTo(new BigDecimal("1.75"));
        assertThat(saved.getGracePeriodDays()).isEqualTo(5);
        // Davranış tespiti: yeni oluşturulan config'te isActive set EDİLMEZ (false kalır);
        // bu yüzden findByCompanyCompanyIdAndIsActiveTrue onu bir daha bulamaz.
        assertThat(saved.isActive()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void saveConfig_whenActiveExists_updatesRateAndGraceDays() {
        LateFeeConfig existing = configStub("2.00", 7);
        existing.setConfigId(33L);
        when(lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(lateFeeConfigRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        LateFeeConfig result = service.saveConfig(COMPANY_ID, new BigDecimal("4.00"), 15);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getMonthlyRate()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(existing.getGracePeriodDays()).isEqualTo(15);
        verify(lateFeeConfigRepository).save(existing);
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private Invoice invoiceStub(PaymentStatus status, LocalDate dueDate, String totalAmount) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setPaymentStatus(status);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(totalAmount != null ? new BigDecimal(totalAmount) : null);
        return invoice;
    }

    private LateFeeConfig configStub(String monthlyRate, int gracePeriodDays) {
        Company company = new Company();
        company.setCompanyId(COMPANY_ID);
        return LateFeeConfig.builder()
                .company(company)
                .monthlyRate(new BigDecimal(monthlyRate))
                .gracePeriodDays(gracePeriodDays)
                .isActive(true)
                .build();
    }
}
