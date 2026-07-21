package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclaration;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclarationLine;
import com.MuhasebePlus.demo.declaration.repository.WithholdingDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import com.MuhasebePlus.demo.tax.repository.WithholdingTaxCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Muhtasar (withholding) declaration service: aggregation of
 * invoice withholding summaries per tax code, period scoping, regeneration rules
 * and the DRAFT -> FINALIZED lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class WithholdingDeclarationServiceTest {

    @Mock private WithholdingDeclarationRepository repository;
    @Mock private InvoiceVatSummaryRepository vatSummaryRepository;
    @Mock private WithholdingTaxCodeRepository taxCodeRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private WithholdingDeclarationService service;

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

    // ── generate: tevkifat kodu bazında toplama ──────────────────────────────

    @Test
    void generate_whenSummariesShareSameCode_aggregatesIntoSingleLine() {
        stubHappyPath(
                List.of(summary(1L, 9, "1000.00", "200.00"),
                        summary(2L, 9, "500.00", "100.00")),
                List.of(taxCode(9, "602", "Yapım işleri")));

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        assertThat(result.getLines()).hasSize(1);
        WithholdingDeclarationLine line = result.getLines().get(0);
        assertThat(line.getWithholdingTaxCodeId()).isEqualTo(9);
        assertThat(line.getWithholdingCode()).isEqualTo("602");
        assertThat(line.getDescription()).isEqualTo("Yapım işleri");
        assertThat(line.getBaseAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(line.getWithholdingAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(line.getDocumentCount()).isEqualTo(2);
        assertThat(line.getLineOrder()).isZero();
    }

    @Test
    void generate_whenSingleInvoiceHasMultipleCodes_createsSeparateLinePerCode() {
        // Aynı fatura (invoiceId=5) iki farklı tevkifat koduna sahip iki özet satırı üretir
        stubHappyPath(
                List.of(summary(5L, 1, "1000.00", "90.00"),
                        summary(5L, 2, "2000.00", "400.00")),
                List.of(taxCode(1, "601", "Hizmet"), taxCode(2, "602", "Yapım işleri")));

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        assertThat(result.getLines()).hasSize(2);
        assertThat(result.getLines())
                .extracting(WithholdingDeclarationLine::getWithholdingTaxCodeId)
                .containsExactly(1, 2);
        assertThat(result.getLines())
                .extracting(WithholdingDeclarationLine::getLineOrder)
                .containsExactly(0, 1);
        assertThat(result.getLines().get(0).getBaseAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getLines().get(1).getBaseAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        // documentCount fatura değil özet satırı sayar: kod başına 1
        assertThat(result.getLines())
                .extracting(WithholdingDeclarationLine::getDocumentCount)
                .containsExactly(1, 1);
    }

    @Test
    void generate_whenTaxCodeNotFoundInRepository_fallsBackToCodeIdAndDefaultDescription() {
        stubHappyPath(
                List.of(summary(1L, 7, "100.00", "10.00")),
                List.of()); // kod tablosunda eşleşme yok

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        WithholdingDeclarationLine line = result.getLines().get(0);
        assertThat(line.getWithholdingCode()).isEqualTo("7");
        assertThat(line.getDescription()).isEqualTo("Tevkifat kodu: 7");
    }

    @Test
    void generate_whenWithholdingAmountsAreNull_treatsThemAsZero() {
        stubHappyPath(
                List.of(summary(1L, 9, null, null),
                        summary(2L, 9, "250.00", "50.00")),
                List.of(taxCode(9, "602", "Yapım işleri")));

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        WithholdingDeclarationLine line = result.getLines().get(0);
        assertThat(line.getBaseAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(line.getWithholdingAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(line.getDocumentCount()).isEqualTo(2);
    }

    @Test
    void generate_computesDeclarationTotalsAcrossAllCodeLines() {
        stubHappyPath(
                List.of(summary(1L, 1, "100.00", "10.00"),
                        summary(2L, 2, "200.00", "40.00")),
                List.of(taxCode(1, "601", "Hizmet"), taxCode(2, "602", "Yapım işleri")));

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        assertThat(result.getTotalWithholdingBase()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.getTotalWithholdingAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.DRAFT);
        assertThat(result.getYear()).isEqualTo(YEAR);
        assertThat(result.getMonth()).isEqualTo(MONTH);
    }

    // ── generate: dönem filtreleme / boş dönem ───────────────────────────────

    @Test
    void generate_requestsWithholdingSummariesForExactPeriodOnly() {
        stubHappyPath(List.of(), List.of());

        service.generate(YEAR, MONTH);

        verify(vatSummaryRepository).findWithholdingsForPeriod(COMPANY_ID, YEAR, MONTH);
        verifyNoMoreInteractions(vatSummaryRepository);
    }

    @Test
    void generate_whenPeriodHasNoWithholdings_createsDraftWithZeroTotalsAndNoLines() {
        stubHappyPath(List.of(), List.of());

        WithholdingDeclaration result = service.generate(YEAR, MONTH);

        assertThat(result.getLines()).isEmpty();
        assertThat(result.getTotalWithholdingBase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalWithholdingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.DRAFT);
        verifyNoInteractions(taxCodeRepository);
    }

    // ── generate: mevcut beyanname davranışı ─────────────────────────────────

    @Test
    void generate_whenExistingDraftForSamePeriod_softDeletesItBeforeRegenerating() {
        WithholdingDeclaration existing = existingDeclaration(DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, YEAR, MONTH))
                .thenReturn(Optional.of(existing));
        when(vatSummaryRepository.findWithholdingsForPeriod(COMPANY_ID, YEAR, MONTH))
                .thenReturn(List.of());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(WithholdingDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(YEAR, MONTH);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        // bir kez mevcut taslağı silmek, bir kez yeni beyannameyi kaydetmek için
        verify(repository).saveAndFlush(existing);
        verify(repository).save(any(WithholdingDeclaration.class));
    }

    @Test
    void generate_whenPeriodAlreadyFinalized_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, YEAR, MONTH))
                .thenReturn(Optional.of(existingDeclaration(DeclarationStatus.FINALIZED)));

        assertThatThrownBy(() -> service.generate(YEAR, MONTH))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten onaylanmış");

        verifyNoInteractions(vatSummaryRepository);
        verify(repository, never()).save(any());
    }

    // ── finalize ──────────────────────────────────────────────────────────────

    @Test
    void finalize_whenStatusDraft_setsStatusToFinalized() {
        WithholdingDeclaration decl = existingDeclaration(DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(decl));
        when(repository.save(any(WithholdingDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        WithholdingDeclaration result = service.finalize(50L);

        assertThat(result.getStatus()).isEqualTo(DeclarationStatus.FINALIZED);
        verify(repository).save(decl);
    }

    @Test
    void finalize_whenStatusNotDraft_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(existingDeclaration(DeclarationStatus.FINALIZED)));

        assertThatThrownBy(() -> service.finalize(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yalnızca DRAFT");

        verify(repository, never()).save(any());
    }

    // ── delete / getById ──────────────────────────────────────────────────────

    @Test
    void delete_whenStatusDraft_softDeletesDeclaration() {
        WithholdingDeclaration decl = existingDeclaration(DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(decl));

        service.delete(50L);

        assertThat(decl.isDeleted()).isTrue();
        assertThat(decl.getDeletedAt()).isNotNull();
        verify(repository).save(decl);
    }

    @Test
    void delete_whenStatusFinalized_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(50L, COMPANY_ID))
                .thenReturn(Optional.of(existingDeclaration(DeclarationStatus.FINALIZED)));

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

    private void stubHappyPath(List<InvoiceVatSummary> summaries, List<WithholdingTaxCode> codes) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(repository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, YEAR, MONTH))
                .thenReturn(Optional.empty());
        when(vatSummaryRepository.findWithholdingsForPeriod(COMPANY_ID, YEAR, MONTH))
                .thenReturn(summaries);
        if (!summaries.isEmpty()) {
            when(taxCodeRepository.findAllById(any())).thenReturn(codes);
        }
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(repository.save(any(WithholdingDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private InvoiceVatSummary summary(Long invoiceId, Integer codeId, String base, String amount) {
        return InvoiceVatSummary.builder()
                .invoiceId(invoiceId)
                .withholdingTaxCodeId(codeId)
                .withholdingBaseAmount(base != null ? new BigDecimal(base) : null)
                .withholdingAmount(amount != null ? new BigDecimal(amount) : null)
                .build();
    }

    private WithholdingTaxCode taxCode(Integer codeId, String code, String description) {
        WithholdingTaxCode taxCode = new WithholdingTaxCode();
        taxCode.setCodeId(codeId);
        taxCode.setCode(code);
        taxCode.setDescription(description);
        return taxCode;
    }

    private WithholdingDeclaration existingDeclaration(DeclarationStatus status) {
        return WithholdingDeclaration.builder()
                .declarationId(50L)
                .company(company)
                .year(YEAR)
                .month(MONTH)
                .status(status)
                .build();
    }
}
