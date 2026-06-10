package com.MuhasebePlus.demo.vat.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.vat.dto.VatCalculateRequestDto;
import com.MuhasebePlus.demo.vat.dto.VatPeriodResponseDto;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import com.MuhasebePlus.demo.vat.repository.VatDeclarationLineRepository;
import com.MuhasebePlus.demo.vat.repository.VatPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VatDeclarationServiceTest {

    @Mock private VatPeriodRepository vatPeriodRepository;
    @Mock private VatDeclarationLineRepository vatDeclarationLineRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountingPeriodGuard periodGuard;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private VatDeclarationService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void calculate_usesTaxableBaseAndPreviousCarriedForward() {
        Invoice sale = invoice(1L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        Invoice purchase = invoice(2L, InvoiceType.purchase, LocalDate.of(2026, 5, 11));
        VatPeriod previous = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(4)
                .carriedForwardVat(new BigDecimal("75.00"))
                .build();

        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.empty());
        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 4))
                .thenReturn(Optional.of(previous));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(sale, purchase));
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(line(2, "100.00", "10.00", "20.00", "999.00")));
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(2L, COMPANY_ID))
                .thenReturn(List.of(line(1, "50.00", null, "20.00", "60.00")));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(vatPeriodRepository.save(any())).thenAnswer(inv -> {
            VatPeriod p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(vatDeclarationLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        VatPeriodResponseDto result = service.calculate(new VatCalculateRequestDto(2026, 5, null));

        assertThat(result.totalOutputVat()).isEqualByComparingTo("36.00");
        assertThat(result.totalInputVat()).isEqualByComparingTo("10.00");
        assertThat(result.previousCarriedForwardVat()).isEqualByComparingTo("75.00");
        assertThat(result.netPayableVat()).isEqualByComparingTo("0.00");
        assertThat(result.carriedForwardVat()).isEqualByComparingTo("49.00");
        assertThat(result.lines()).anySatisfy(line ->
                assertThat(line.taxBase()).isEqualByComparingTo("180.00"));
    }

    @Test
    void calculate_whenVatPeriodLocked_throwsBusinessException() {
        VatPeriod locked = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .status("LOCKED")
                .build();
        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.calculate(new VatCalculateRequestDto(2026, 5, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kilitli");

        verify(invoiceRepository, never()).findByCompanyCompanyIdAndIsDeletedFalse(anyLong());
    }

    @Test
    void lock_createsVatSettlementJournalAndStoresLockMetadata() {
        VatPeriod period = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .status("DRAFT")
                .totalOutputVat(new BigDecimal("100.00"))
                .totalInputVat(new BigDecimal("40.00"))
                .netPayableVat(new BigDecimal("60.00"))
                .build();
        period.setId(10L);

        when(vatPeriodRepository.findByIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(period));
        when(companyContext.getCurrentUserId()).thenReturn(77L);
        when(journalEntryService.createForVatSettlement(period)).thenReturn(500L);
        when(vatPeriodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VatPeriodResponseDto result = service.lock(10L);

        assertThat(result.status()).isEqualTo("LOCKED");
        assertThat(result.settlementJournalEntryId()).isEqualTo(500L);
        assertThat(result.lockedBy()).isEqualTo(77L);
        assertThat(result.lockedAt()).isNotNull();
        verify(periodGuard).assertOpen(LocalDate.of(2026, 5, 31));
    }

    private Invoice invoice(Long id, InvoiceType type, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(date);
        invoice.setPaymentStatus(PaymentStatus.pending);
        invoice.setCancelled(false);
        return invoice;
    }

    private InvoiceLineItem line(int quantity, String unitPrice, String discountRate, String vatRate, String lineTotal) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setDiscountRate(discountRate != null ? new BigDecimal(discountRate) : null);
        line.setVatRate(new BigDecimal(vatRate));
        line.setLineTotal(new BigDecimal(lineTotal));
        return line;
    }
}
