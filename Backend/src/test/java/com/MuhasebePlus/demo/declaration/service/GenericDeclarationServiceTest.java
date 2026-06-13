package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.GenericDeclaration;
import com.MuhasebePlus.demo.declaration.repository.GenericDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.MuhasebePlus.demo.declaration.service.GenericDeclarationService.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the generic declaration service (KDV2, geçici vergi, kurumlar
 * vergisi): snapshot computation, tax-rate arithmetic and the
 * DRAFT -> FINALIZED -> SUBMITTED state machine.
 */
@ExtendWith(MockitoExtension.class)
class GenericDeclarationServiceTest {

    @Mock private GenericDeclarationRepository repository;
    @Mock private InvoiceVatSummaryRepository vatSummaryRepository;
    @Mock private JournalEntryLineRepository journalLineRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private GenericDeclarationService service;

    private static final Long COMPANY_ID = 1L;
    private static final int YEAR = 2026;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");
    }

    // ── generate: tip doğrulama ───────────────────────────────────────────────

    @Test
    void generate_whenTypeUnknown_throwsBusinessException() {
        // validateType, companyContext'e erişilmeden önce çalışır
        assertThatThrownBy(() -> service.generate("DAMGA", YEAR, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Geçersiz beyanname tipi");

        verifyNoInteractions(companyContext, repository, vatSummaryRepository, journalLineRepository);
    }

    @Test
    void list_whenTypeUnknown_throwsBusinessException() {
        assertThatThrownBy(() -> service.list("KDV9"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Geçersiz beyanname tipi");
    }

    // ── generate: KDV2 ────────────────────────────────────────────────────────

    @Test
    void generate_kdv2_aggregatesPurchaseWithholdingsByCodeAndSetsTotalPayable() {
        stubNoExistingDeclaration(KDV2, YEAR, 5);
        when(vatSummaryRepository.findPurchaseWithholdingsForPeriod(COMPANY_ID, YEAR, 5))
                .thenReturn(List.of(
                        summary(9, "1000.00", "90.00"),
                        summary(9, "200.00", "18.00"),
                        summary(11, "500.00", "25.00")));

        GenericDeclaration result = service.generate(KDV2, YEAR, 5);

        assertThat(result.getDeclarationType()).isEqualTo(KDV2);
        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.DRAFT);
        assertThat(result.getYear()).isEqualTo(YEAR);
        assertThat(result.getPeriod()).isEqualTo(5);
        // totalPayable = toplam tevkifat tutarı
        assertThat(result.getTotalPayable()).isEqualByComparingTo(new BigDecimal("133.00"));

        Map<String, Object> snapshot = result.getDataSnapshot();
        assertThat((BigDecimal) snapshot.get("totalBase")).isEqualByComparingTo(new BigDecimal("1700.00"));
        assertThat((BigDecimal) snapshot.get("totalWithholding")).isEqualByComparingTo(new BigDecimal("133.00"));

        @SuppressWarnings("unchecked")
        Map<String, Object> lines = (Map<String, Object>) snapshot.get("lines");
        assertThat(lines).containsOnlyKeys("code_9", "code_11");
        @SuppressWarnings("unchecked")
        Map<String, Object> code9 = (Map<String, Object>) lines.get("code_9");
        assertThat((BigDecimal) code9.get("baseAmount")).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat((BigDecimal) code9.get("withholdingAmount")).isEqualByComparingTo(new BigDecimal("108.00"));
    }

    @Test
    void generate_kdv2_whenPeriodHasNoWithholdings_createsDraftWithZeroPayable() {
        stubNoExistingDeclaration(KDV2, YEAR, 5);
        when(vatSummaryRepository.findPurchaseWithholdingsForPeriod(COMPANY_ID, YEAR, 5))
                .thenReturn(List.of());

        GenericDeclaration result = service.generate(KDV2, YEAR, 5);

        assertThat(result.getTotalPayable()).isEqualByComparingTo(BigDecimal.ZERO);
        @SuppressWarnings("unchecked")
        Map<String, Object> lines = (Map<String, Object>) result.getDataSnapshot().get("lines");
        assertThat(lines).isEmpty();
    }

    // ── generate: geçici vergi ────────────────────────────────────────────────

    @Test
    void generate_temporaryTax_whenQuarterOutOfRange_throwsBusinessException() {
        stubExistingLookupOnly(TEMPORARY_TAX, YEAR, 5);

        assertThatThrownBy(() -> service.generate(TEMPORARY_TAX, YEAR, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-4");

        verifyNoInteractions(journalLineRepository);
        verify(repository, never()).save(any());
    }

    @Test
    void generate_temporaryTax_computesQuarterRangeAndFifteenPercentTax() {
        stubNoExistingDeclaration(TEMPORARY_TAX, YEAR, 2);
        when(journalLineRepository.sumByAccountTypeForPeriod(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        row(AccountType.INCOME, "0.00", "1000.00"),
                        row(AccountType.EXPENSE, "200.00", "0.00"),
                        row(AccountType.COST, "100.00", "0.00")));

        GenericDeclaration result = service.generate(TEMPORARY_TAX, YEAR, 2);

        // 2. çeyrek: 1 Nisan - 30 Haziran
        verify(journalLineRepository).sumByAccountTypeForPeriod(
                COMPANY_ID, LocalDate.of(YEAR, 4, 1), LocalDate.of(YEAR, 6, 30),
                List.of(AccountType.INCOME, AccountType.EXPENSE, AccountType.COST));

        Map<String, Object> snapshot = result.getDataSnapshot();
        assertThat((BigDecimal) snapshot.get("totalIncome")).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat((BigDecimal) snapshot.get("totalExpense")).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat((BigDecimal) snapshot.get("totalCost")).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat((BigDecimal) snapshot.get("netProfit")).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(snapshot.get("taxRate")).isEqualTo(0.15);
        // 700 * 0.15 = 105.00
        assertThat(result.getTotalPayable()).isEqualByComparingTo(new BigDecimal("105.00"));
    }

    @Test
    void generate_temporaryTax_whenNetLoss_clampsTaxBaseToZero() {
        stubNoExistingDeclaration(TEMPORARY_TAX, YEAR, 1);
        when(journalLineRepository.sumByAccountTypeForPeriod(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        row(AccountType.INCOME, "0.00", "100.00"),
                        row(AccountType.EXPENSE, "500.00", "0.00")));

        GenericDeclaration result = service.generate(TEMPORARY_TAX, YEAR, 1);

        Map<String, Object> snapshot = result.getDataSnapshot();
        assertThat((BigDecimal) snapshot.get("netProfit")).isEqualByComparingTo(new BigDecimal("-400.00"));
        assertThat((BigDecimal) snapshot.get("taxBase")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalPayable()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── generate: kurumlar vergisi ────────────────────────────────────────────

    @Test
    void generate_corporateTax_computesFullYearRangeAndTwentyFivePercentTax() {
        stubNoExistingDeclaration(CORPORATE_TAX, YEAR, 0);
        when(journalLineRepository.sumByAccountTypeForPeriod(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        row(AccountType.INCOME, "0.00", "2000.00"),
                        row(AccountType.EXPENSE, "400.00", "0.00")));

        GenericDeclaration result = service.generate(CORPORATE_TAX, YEAR, 0);

        verify(journalLineRepository).sumByAccountTypeForPeriod(
                COMPANY_ID, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31),
                List.of(AccountType.INCOME, AccountType.EXPENSE, AccountType.COST));

        // (2000 - 400) * 0.25 = 400.00
        assertThat(result.getTotalPayable()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.getDataSnapshot().get("taxRate")).isEqualTo(0.25);
        assertThat(result.getDataSnapshot().get("period")).isEqualTo(0);
    }

    // ── generate: mevcut beyanname davranışı ─────────────────────────────────

    @Test
    void generate_whenExistingDraftForSamePeriod_softDeletesItBeforeRegenerating() {
        GenericDeclaration existing = existingDeclaration(KDV2, DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
                COMPANY_ID, KDV2, YEAR, 5)).thenReturn(Optional.of(existing));
        when(vatSummaryRepository.findPurchaseWithholdingsForPeriod(COMPANY_ID, YEAR, 5))
                .thenReturn(List.of());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(GenericDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(KDV2, YEAR, 5);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(repository).saveAndFlush(existing);
        verify(repository).save(any(GenericDeclaration.class));
    }

    @Test
    void generate_whenExistingFinalizedForSamePeriod_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
                COMPANY_ID, KDV2, YEAR, 5))
                .thenReturn(Optional.of(existingDeclaration(KDV2, DeclarationStatus.FINALIZED)));

        assertThatThrownBy(() -> service.generate(KDV2, YEAR, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten onaylanmış");

        verifyNoInteractions(vatSummaryRepository);
        verify(repository, never()).save(any());
    }

    // ── durum geçişleri: finalize / markSubmitted / delete ───────────────────

    @Test
    void finalize_whenStatusDraft_transitionsToFinalized() {
        GenericDeclaration decl = existingDeclaration(KDV2, DeclarationStatus.DRAFT);
        stubFindById(decl);
        when(repository.save(any(GenericDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        GenericDeclaration result = service.finalize(50L);

        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.FINALIZED);
        verify(repository).save(decl);
    }

    @Test
    void finalize_whenStatusNotDraft_throwsBusinessException() {
        stubFindById(existingDeclaration(KDV2, DeclarationStatus.SUBMITTED));

        assertThatThrownBy(() -> service.finalize(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yalnızca DRAFT");

        verify(repository, never()).save(any());
    }

    @Test
    void markSubmitted_whenStatusFinalized_transitionsToSubmitted() {
        GenericDeclaration decl = existingDeclaration(KDV2, DeclarationStatus.FINALIZED);
        stubFindById(decl);
        when(repository.save(any(GenericDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        GenericDeclaration result = service.markSubmitted(50L);

        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.SUBMITTED);
        verify(repository).save(decl);
    }

    @Test
    void markSubmitted_whenStatusDraft_throwsBusinessException() {
        stubFindById(existingDeclaration(KDV2, DeclarationStatus.DRAFT));

        assertThatThrownBy(() -> service.markSubmitted(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yalnızca FINALIZED");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_whenStatusDraft_softDeletesDeclaration() {
        GenericDeclaration decl = existingDeclaration(KDV2, DeclarationStatus.DRAFT);
        stubFindById(decl);

        service.delete(50L);

        assertThat(decl.isDeleted()).isTrue();
        assertThat(decl.getDeletedAt()).isNotNull();
        verify(repository).save(decl);
    }

    @Test
    void delete_whenStatusFinalized_throwsBusinessException() {
        stubFindById(existingDeclaration(KDV2, DeclarationStatus.FINALIZED));

        assertThatThrownBy(() -> service.delete(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yalnızca DRAFT");

        verify(repository, never()).save(any());
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

    private void stubNoExistingDeclaration(String type, int year, int period) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
                COMPANY_ID, type, year, period)).thenReturn(Optional.empty());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(GenericDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubExistingLookupOnly(String type, int year, int period) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
                COMPANY_ID, type, year, period)).thenReturn(Optional.empty());
    }

    private void stubFindById(GenericDeclaration decl) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(decl));
    }

    private InvoiceVatSummary summary(Integer codeId, String base, String amount) {
        return InvoiceVatSummary.builder()
                .invoiceId(1L)
                .withholdingTaxCodeId(codeId)
                .withholdingBaseAmount(base != null ? new BigDecimal(base) : null)
                .withholdingAmount(amount != null ? new BigDecimal(amount) : null)
                .build();
    }

    private GenericDeclaration existingDeclaration(String type, DeclarationStatus status) {
        return GenericDeclaration.builder()
                .declarationId(50L)
                .company(company)
                .declarationType(type)
                .year(YEAR)
                .period(5)
                .status(status)
                .build();
    }

    private Object[] row(AccountType type, String debit, String credit) {
        return new Object[]{type, new BigDecimal(debit), new BigDecimal(credit)};
    }
}
