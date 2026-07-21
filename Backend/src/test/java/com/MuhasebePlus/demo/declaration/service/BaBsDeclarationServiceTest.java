package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclaration;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclarationLine;
import com.MuhasebePlus.demo.declaration.repository.BaBsDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
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

/**
 * Unit tests for the Ba-Bs form service: the 5.000 TL reporting threshold
 * (inclusive, ">=" semantics), per-counterparty aggregation, BA/BS type to
 * invoice-type mapping and calendar-month period boundaries.
 *
 * Note: cancelled/deleted invoice exclusion lives in the JPQL of
 * InvoiceRepository.findForPeriod ("cancelled = false AND isDeleted = false"),
 * not in this service; these tests verify the service consults only that query.
 */
@ExtendWith(MockitoExtension.class)
class BaBsDeclarationServiceTest {

    @Mock private BaBsDeclarationRepository repository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private BaBsDeclarationService service;

    private static final Long COMPANY_ID = 1L;
    private static final int YEAR = 2026;
    private static final int MONTH = 5;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");
    }

    // ── generate: 5.000 TL eşiği sınır durumları ─────────────────────────────

    @Test
    void generate_whenCustomerTotalExactlyFiveThousand_includesLine() {
        // compareTo(THRESHOLD) >= 0 : tam 5.000,00 TL dahil edilir
        stubHappyPath("BS", YEAR, MONTH, List.of(invoice(1L, "5000.00")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().get(0).getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getRecordCount()).isEqualTo(1);
    }

    @Test
    void generate_whenCustomerTotalJustBelowFiveThousand_excludesLine() {
        stubHappyPath("BS", YEAR, MONTH, List.of(invoice(1L, "4999.99")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).isEmpty();
        assertThat(result.getRecordCount()).isZero();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generate_whenCustomerTotalJustAboveFiveThousand_includesLine() {
        stubHappyPath("BS", YEAR, MONTH, List.of(invoice(1L, "5000.01")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().get(0).getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("5000.01"));
    }

    @Test
    void generate_aggregatesInvoiceSubtotalsPerCustomerBeforeApplyingThreshold() {
        // Tek başına eşik altı iki fatura, toplamda eşiği aştığı için bildirilir
        stubHappyPath("BS", YEAR, MONTH,
                List.of(invoice(1L, "3000.00"), invoice(1L, "2500.00")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).hasSize(1);
        BaBsDeclarationLine line = result.getLines().get(0);
        assertThat(line.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5500.00"));
        assertThat(line.getDocumentCount()).isEqualTo(2);
    }

    // ── generate: cari (müşteri) bazında gruplama ────────────────────────────

    @Test
    void generate_groupsByCustomerId_andKeepsOnlyCustomersOverThreshold() {
        stubHappyPath("BS", YEAR, MONTH, List.of(
                invoice(1L, "6000.00"),
                invoice(2L, "7000.00"),
                invoice(3L, "100.00"))); // eşik altı, elenir

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).hasSize(2);
        assertThat(result.getLines())
                .extracting(BaBsDeclarationLine::getCustomerName)
                .containsExactly("Müşteri #1", "Müşteri #2");
        // VKN doldurulmuyor: kod taxNumber alanını boş string olarak yazar
        assertThat(result.getLines())
                .extracting(BaBsDeclarationLine::getTaxNumber)
                .containsOnly("");
        assertThat(result.getRecordCount()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("13000.00"));
    }

    @Test
    void generate_whenInvoiceHasNoCustomerId_skipsInvoice() {
        stubHappyPath("BS", YEAR, MONTH, List.of(invoice(null, "9000.00")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).isEmpty();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generate_whenInvoiceSubtotalIsNull_treatsItAsZero() {
        stubHappyPath("BS", YEAR, MONTH,
                List.of(invoice(1L, null), invoice(1L, "5000.00")));

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().get(0).getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getLines().get(0).getDocumentCount()).isEqualTo(2);
    }

    // ── generate: BA / BS ayrımı ve dönem filtresi ───────────────────────────

    @Test
    void generate_whenTypeBs_queriesSaleInvoicesForCalendarMonth() {
        stubHappyPath("BS", YEAR, MONTH, List.of());

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BS");

        assertThat(result.getBabsType()).isEqualTo("BS");
        verify(invoiceRepository).findForPeriod(COMPANY_ID, InvoiceType.sale,
                LocalDate.of(YEAR, MONTH, 1), LocalDate.of(YEAR, MONTH, 31));
    }

    @Test
    void generate_whenTypeBa_queriesPurchaseInvoicesForCalendarMonth() {
        stubHappyPath("BA", YEAR, MONTH, List.of());

        BaBsDeclaration result = service.generate(YEAR, MONTH, "BA");

        assertThat(result.getBabsType()).isEqualTo("BA");
        verify(invoiceRepository).findForPeriod(COMPANY_ID, InvoiceType.purchase,
                LocalDate.of(YEAR, MONTH, 1), LocalDate.of(YEAR, MONTH, 31));
    }

    @Test
    void generate_whenFebruaryOfLeapYear_periodEndsOnTwentyNinth() {
        stubHappyPath("BA", 2024, 2, List.of());

        service.generate(2024, 2, "BA");

        verify(invoiceRepository).findForPeriod(COMPANY_ID, InvoiceType.purchase,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
    }

    @Test
    void generate_whenTypeInvalid_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        assertThatThrownBy(() -> service.generate(YEAR, MONTH, "BX"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Geçersiz form tipi");

        verifyNoInteractions(invoiceRepository);
        verify(repository, never()).save(any());
    }

    // ── generate: mevcut form davranışı ──────────────────────────────────────

    @Test
    void generate_whenExistingDraftForSamePeriod_softDeletesItBeforeRegenerating() {
        BaBsDeclaration existing = existingDeclaration("BS", DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndBabsTypeAndYearAndMonthAndIsDeletedFalse(
                COMPANY_ID, "BS", YEAR, MONTH)).thenReturn(Optional.of(existing));
        when(invoiceRepository.findForPeriod(eq(COMPANY_ID), eq(InvoiceType.sale),
                any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(BaBsDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(YEAR, MONTH, "BS");

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(repository, times(2)).save(any(BaBsDeclaration.class));
    }

    @Test
    void generate_whenPeriodAlreadyFinalized_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndBabsTypeAndYearAndMonthAndIsDeletedFalse(
                COMPANY_ID, "BS", YEAR, MONTH))
                .thenReturn(Optional.of(existingDeclaration("BS", DeclarationStatus.FINALIZED)));

        assertThatThrownBy(() -> service.generate(YEAR, MONTH, "BS"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten onaylanmış");

        verifyNoInteractions(invoiceRepository);
        verify(repository, never()).save(any());
    }

    // ── finalize / delete / getById ──────────────────────────────────────────

    @Test
    void finalize_whenStatusDraft_setsStatusToFinalized() {
        BaBsDeclaration decl = existingDeclaration("BS", DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(decl));
        when(repository.save(any(BaBsDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        BaBsDeclaration result = service.finalize(50L);

        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.FINALIZED);
        verify(repository).save(decl);
    }

    @Test
    void finalize_whenStatusNotDraft_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(existingDeclaration("BS", DeclarationStatus.FINALIZED)));

        assertThatThrownBy(() -> service.finalize(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yalnızca DRAFT");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_whenStatusDraft_softDeletesForm() {
        BaBsDeclaration decl = existingDeclaration("BA", DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(decl));

        service.delete(50L);

        assertThat(decl.isDeleted()).isTrue();
        assertThat(decl.getDeletedAt()).isNotNull();
        verify(repository).save(decl);
    }

    @Test
    void getById_whenNotFoundForCompany_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bulunamadı");
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private void stubHappyPath(String type, int year, int month, List<Invoice> invoices) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndBabsTypeAndYearAndMonthAndIsDeletedFalse(
                COMPANY_ID, type, year, month)).thenReturn(Optional.empty());
        when(invoiceRepository.findForPeriod(eq(COMPANY_ID), any(InvoiceType.class),
                any(LocalDate.class), any(LocalDate.class))).thenReturn(invoices);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(BaBsDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Invoice invoice(Long customerId, String subtotal) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setSubtotal(subtotal != null ? new BigDecimal(subtotal) : null);
        return invoice;
    }

    private BaBsDeclaration existingDeclaration(String type, DeclarationStatus status) {
        return BaBsDeclaration.builder()
                .declarationId(50L)
                .company(company)
                .babsType(type)
                .year(YEAR)
                .month(MONTH)
                .status(status)
                .build();
    }
}
