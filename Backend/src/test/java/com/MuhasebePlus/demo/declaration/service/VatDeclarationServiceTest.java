package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.dto.GenerateDeclarationRequestDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationLineDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.VatDeclaration;
import com.MuhasebePlus.demo.declaration.repository.VatDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for the declaration-module KDV1 service (period aggregation of
 * calculated vs deductible VAT, carry-forward, payable/refundable outcome).
 *
 * Note: this intentionally does NOT overlap with
 * com.MuhasebePlus.demo.vat.service.VatDeclarationServiceTest which covers the
 * separate VatPeriod-based module (lock/settlement journal flow).
 */
@ExtendWith(MockitoExtension.class)
class VatDeclarationServiceTest {

    @Mock private VatDeclarationRepository declarationRepository;
    @Mock private InvoiceVatSummaryRepository vatSummaryRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private VatDeclarationService service;

    private static final Long COMPANY_ID = 1L;
    private static final String KDV1 = "KDV1";
    private static final int YEAR = 2026;
    private static final int MONTH = 5;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");
        company.setTaxNumber("1234567890");
        company.setTaxOffice("Kadıköy");
    }

    // ── generate: hesaplanan / indirilecek KDV toplama ───────────────────────

    @Test
    void generate_whenSalesAndPurchasesExist_aggregatesCalculatedAndDeductibleVat() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "1000.00", "200.00"), summary("10", "500.00", "50.00")),
                List.of(summary("20", "300.00", "60.00"), summary("10", "100.00", "10.00")));

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        assertThat(result.totalSalesBase()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.totalSalesVat()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.totalPurchasesBase()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.totalPurchasesVat()).isEqualByComparingTo(new BigDecimal("70.00"));
        // 250 - 70 - 0 devreden = 180 ödenecek
        assertThat(result.payableVat()).isEqualByComparingTo(new BigDecimal("180.00"));
        assertThat(result.nextPeriodCarryover()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.status()).isEqualTo(DeclarationStatus.DRAFT);
    }

    @Test
    void generate_whenMultipleSummariesShareSameRate_mergesBaseAndVatPerRate() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "100.00", "20.00"), summary("20", "200.00", "40.00")),
                List.of());

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        List<VatDeclarationLineDto> rateLines = result.lines().stream()
                .filter(l -> l.lineCode().equals("39"))
                .toList();
        assertThat(rateLines).hasSize(1);
        assertThat(rateLines.get(0).baseAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(rateLines.get(0).vatAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    // ── generate: ödenecek / devreden sonucu ─────────────────────────────────

    @Test
    void generate_whenCalculatedVatExceedsDeductible_setsPayableAndZeroCarryover() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "1000.00", "200.00")),
                List.of(summary("20", "250.00", "50.00")));

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        assertThat(result.payableVat()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.nextPeriodCarryover()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generate_whenDeductibleVatExceedsCalculated_carriesForwardDifference() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "250.00", "50.00")),
                List.of(summary("20", "1000.00", "200.00")));

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        assertThat(result.payableVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.nextPeriodCarryover()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void generate_whenNetIsExactlyZero_reportsZeroPayableAndZeroCarryover() {
        // Sınır durumu: net == 0 "devreden" dalına düşer; her iki tutar da sıfır olmalı
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "500.00", "100.00")),
                List.of(summary("20", "500.00", "100.00")));

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        assertThat(result.payableVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.nextPeriodCarryover()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── generate: devreden KDV ───────────────────────────────────────────────

    @Test
    void generate_whenRequestProvidesCarryover_subtractsItWithoutAutoLookup() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "500.00", "100.00")),
                List.of(summary("20", "100.00", "20.00")));

        VatDeclarationResponseDto result =
                service.generate(request(YEAR, MONTH, new BigDecimal("30.00")));

        assertThat(result.previousPeriodCarryover()).isEqualByComparingTo(new BigDecimal("30.00"));
        // 100 - 20 - 30 = 50
        assertThat(result.payableVat()).isEqualByComparingTo(new BigDecimal("50.00"));
        verify(declarationRepository, never())
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, MONTH - 1);
    }

    @Test
    void generate_whenRequestCarryoverIsZero_autoFetchesPreviousPeriodCarryover() {
        // Davranış notu: istemci açıkça 0 gönderse bile otomatik devir araması yapılır
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "500.00", "100.00")),
                List.of());
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, MONTH))
                .thenReturn(Optional.empty());
        VatDeclaration previous = VatDeclaration.builder()
                .declarationType(KDV1).year(YEAR).month(MONTH - 1)
                .nextPeriodCarryover(new BigDecimal("80.00"))
                .build();
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, MONTH - 1))
                .thenReturn(Optional.of(previous));

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, BigDecimal.ZERO));

        assertThat(result.previousPeriodCarryover()).isEqualByComparingTo(new BigDecimal("80.00"));
        // 100 - 0 - 80 = 20
        assertThat(result.payableVat()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void generate_whenJanuary_looksUpDecemberOfPreviousYearForCarryover() {
        // Dönem sınırı: Ocak ayının önceki dönemi geçen yılın Aralık ayıdır
        stubGenerateHappyPath(YEAR, 1,
                List.of(summary("20", "500.00", "100.00")),
                List.of());
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, 1))
                .thenReturn(Optional.empty());
        VatDeclaration december = VatDeclaration.builder()
                .declarationType(KDV1).year(YEAR - 1).month(12)
                .nextPeriodCarryover(new BigDecimal("40.00"))
                .build();
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR - 1, 12))
                .thenReturn(Optional.of(december));

        VatDeclarationResponseDto result = service.generate(request(YEAR, 1, null));

        assertThat(result.previousPeriodCarryover()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.payableVat()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    // ── generate: boş dönem ──────────────────────────────────────────────────

    @Test
    void generate_whenPeriodHasNoInvoices_producesZeroTotalsAndOnlyFixedLines() {
        stubGenerateHappyPath(YEAR, MONTH, List.of(), List.of());

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        assertThat(result.totalSalesBase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalSalesVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPurchasesBase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPurchasesVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.payableVat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.nextPeriodCarryover()).isEqualByComparingTo(BigDecimal.ZERO);
        // Oran satırı yok; sadece sabit satırlar: 60, T2_100, T2_105
        assertThat(result.lines()).extracting(VatDeclarationLineDto::lineCode)
                .containsExactly("60", "T2_100", "T2_105");
    }

    // ── generate: mevcut beyanname davranışı ─────────────────────────────────

    @Test
    void generate_whenDraftExistsForPeriod_softDeletesItBeforeRegenerating() {
        VatDeclaration existingDraft = VatDeclaration.builder()
                .declarationId(7L).declarationType(KDV1)
                .year(YEAR).month(MONTH).status(DeclarationStatus.DRAFT)
                .build();
        stubGenerateHappyPath(YEAR, MONTH, List.of(), List.of());
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, MONTH))
                .thenReturn(Optional.of(existingDraft));

        service.generate(request(YEAR, MONTH, new BigDecimal("10.00")));

        ArgumentCaptor<VatDeclaration> captor = ArgumentCaptor.forClass(VatDeclaration.class);
        verify(declarationRepository).saveAndFlush(existingDraft);
        assertThat(existingDraft.isDeleted()).isTrue();
        assertThat(existingDraft.getDeletedAt()).isNotNull();
        verify(declarationRepository).save(captor.capture());
        VatDeclaration regenerated = captor.getValue();
        assertThat(regenerated.getStatus()).isEqualTo(DeclarationStatus.DRAFT);
        assertThat(regenerated.isDeleted()).isFalse();
    }

    @Test
    void generate_whenFinalizedDeclarationExistsForPeriod_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        VatDeclaration finalized = VatDeclaration.builder()
                .declarationId(7L).declarationType(KDV1)
                .year(YEAR).month(MONTH).status(DeclarationStatus.FINALIZED)
                .build();
        when(declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
                        COMPANY_ID, KDV1, YEAR, MONTH))
                .thenReturn(Optional.of(finalized));

        assertThatThrownBy(() -> service.generate(request(YEAR, MONTH, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten onaylanmış");

        verify(declarationRepository, never()).save(any());
        verify(vatSummaryRepository, never()).findSalesVatSummaryForPeriod(anyLong(), anyInt(), anyInt());
    }

    // ── generate: satır kodları ──────────────────────────────────────────────

    @Test
    void generate_standardRates_mapToOfficialLineCodes30And35And39() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("1", "100.00", "1.00"),
                        summary("10", "100.00", "10.00"),
                        summary("20", "100.00", "20.00")),
                List.of());

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        // TreeMap oran sırası: %1 → 30, %10 → 35, %20 → 39
        assertThat(result.lines()).extracting(VatDeclarationLineDto::lineCode)
                .startsWith("30", "35", "39");
    }

    @Test
    void generate_whenNonStandardRate_buildsFallbackLineCode() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("18.00", "100.00", "18.00")),
                List.of());

        VatDeclarationResponseDto result = service.generate(request(YEAR, MONTH, null));

        VatDeclarationLineDto rateLine = result.lines().get(0);
        assertThat(rateLine.lineCode()).isEqualTo("318");
        assertThat(rateLine.description()).isEqualTo("%18 KDV'li satışlar");
    }

    @Test
    void generate_setsXmlContentOnSavedDeclaration() {
        stubGenerateHappyPath(YEAR, MONTH,
                List.of(summary("20", "100.00", "20.00")), List.of());

        service.generate(request(YEAR, MONTH, null));

        ArgumentCaptor<VatDeclaration> captor = ArgumentCaptor.forClass(VatDeclaration.class);
        verify(declarationRepository).save(captor.capture());
        assertThat(captor.getValue().getXmlContent())
                .contains("<KDVBeyannamesi>")
                .contains("</KDVBeyannamesi>");
    }

    // ── durum geçişleri ──────────────────────────────────────────────────────

    @Test
    void finalize_whenDraft_marksFinalized() {
        VatDeclaration draft = declarationWithStatus(DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(declarationRepository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.of(draft));
        when(declarationRepository.save(any(VatDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        VatDeclarationResponseDto result = service.finalize(9L);

        assertThat(result.status()).isEqualTo(DeclarationStatus.FINALIZED);
    }

    @Test
    void finalize_whenAlreadyFinalized_throwsBusinessException() {
        VatDeclaration finalized = declarationWithStatus(DeclarationStatus.FINALIZED);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(declarationRepository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.of(finalized));

        assertThatThrownBy(() -> service.finalize(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");

        verify(declarationRepository, never()).save(any());
    }

    @Test
    void markSubmitted_whenFinalized_setsStatusAndSubmittedTimestamp() {
        VatDeclaration finalized = declarationWithStatus(DeclarationStatus.FINALIZED);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(declarationRepository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.of(finalized));
        when(declarationRepository.save(any(VatDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        VatDeclarationResponseDto result = service.markSubmitted(9L);

        assertThat(result.status()).isEqualTo(DeclarationStatus.SUBMITTED);
        assertThat(result.submittedAt()).isNotNull();
    }

    @Test
    void markSubmitted_whenStillDraft_throwsBusinessException() {
        VatDeclaration draft = declarationWithStatus(DeclarationStatus.DRAFT);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(declarationRepository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.markSubmitted(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FINALIZED");

        verify(declarationRepository, never()).save(any());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private void stubGenerateHappyPath(int year, int month,
                                       List<InvoiceVatSummary> sales,
                                       List<InvoiceVatSummary> purchases) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(vatSummaryRepository.findSalesVatSummaryForPeriod(COMPANY_ID, year, month)).thenReturn(sales);
        when(vatSummaryRepository.findPurchaseVatSummaryForPeriod(COMPANY_ID, year, month)).thenReturn(purchases);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(declarationRepository.save(any(VatDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private GenerateDeclarationRequestDto request(int year, int month, BigDecimal carryover) {
        return new GenerateDeclarationRequestDto(year, month, carryover);
    }

    private InvoiceVatSummary summary(String rate, String base, String vat) {
        return InvoiceVatSummary.builder()
                .vatRate(new BigDecimal(rate))
                .vatBaseAmount(new BigDecimal(base))
                .vatAmount(new BigDecimal(vat))
                .build();
    }

    private VatDeclaration declarationWithStatus(DeclarationStatus status) {
        return VatDeclaration.builder()
                .declarationId(9L)
                .company(company)
                .declarationType(KDV1)
                .year(YEAR)
                .month(MONTH)
                .status(status)
                .totalSalesBase(BigDecimal.ZERO)
                .totalSalesVat(BigDecimal.ZERO)
                .totalPurchasesBase(BigDecimal.ZERO)
                .totalPurchasesVat(BigDecimal.ZERO)
                .payableVat(BigDecimal.ZERO)
                .nextPeriodCarryover(BigDecimal.ZERO)
                .build();
    }
}
