package com.MuhasebePlus.demo.edefter.service;

import com.MuhasebePlus.demo.accounting.entity.ChartOfAccount;
import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.entity.JournalEntryLine;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.edefter.entity.EDefterRun;
import com.MuhasebePlus.demo.edefter.entity.EDefterStatus;
import com.MuhasebePlus.demo.edefter.repository.EDefterRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * EDefterService için kapsamlı birim testleri. Repository ve CompanyContext mock'lanır;
 * doGenerate içindeki gerçek EDefterXmlBuilder çağrısı çalıştırılarak XML üretim akışı
 * uçtan uca doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class EDefterServiceTest {

    @Mock private EDefterRunRepository runRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private EDefterService service;

    private static final Long COMPANY_ID = 7L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .companyId(COMPANY_ID)
                .companyName("Test A.Ş.")
                .taxNumber("1234567890")
                .build();
    }

    private JournalEntry sampleEntry() {
        ChartOfAccount kasa = new ChartOfAccount();
        kasa.setAccountCode("100");
        kasa.setAccountName("Kasa");
        ChartOfAccount satis = new ChartOfAccount();
        satis.setAccountCode("600");
        satis.setAccountName("Satış");

        JournalEntryLine l1 = new JournalEntryLine();
        l1.setAccount(kasa);
        l1.setDebitAmount(new BigDecimal("100.00"));
        l1.setCreditAmount(BigDecimal.ZERO);

        JournalEntryLine l2 = new JournalEntryLine();
        l2.setAccount(satis);
        l2.setDebitAmount(BigDecimal.ZERO);
        l2.setCreditAmount(new BigDecimal("100.00"));

        JournalEntry e = new JournalEntry();
        e.setEntryNumber("YV-1");
        e.setEntryDate(LocalDate.of(2026, 3, 10));
        e.setSourceType(JournalSourceType.MANUAL);
        e.setLines(new ArrayList<>(List.of(l1, l2)));
        return e;
    }

    private EDefterRun existingRun(Long runId, int year, int month) {
        return EDefterRun.builder()
                .runId(runId)
                .company(company)
                .year(year)
                .month(month)
                .status(EDefterStatus.GENERATED)
                .build();
    }

    // ── generate ──────────────────────────────────────────────────────────────

    @Test
    void generate_persistsRunWithXmlAndComputedCounts() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, 2026, 3))
                .thenReturn(Optional.empty());
        when(journalEntryRepository
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        eq(COMPANY_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(sampleEntry()));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(runRepository.save(any(EDefterRun.class))).thenAnswer(inv -> inv.getArgument(0));

        EDefterRun result = service.generate(2026, 3);

        ArgumentCaptor<EDefterRun> captor = ArgumentCaptor.forClass(EDefterRun.class);
        verify(runRepository).save(captor.capture());
        EDefterRun saved = captor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getYear()).isEqualTo(2026);
        assertThat(saved.getMonth()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(EDefterStatus.GENERATED);
        assertThat(saved.getJournalEntryCount()).isEqualTo(1);
        assertThat(saved.getJournalLineCount()).isEqualTo(2);
        assertThat(saved.getGeneratedAt()).isNotNull();
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getJournalXml()).contains("<gl-cor:entriesType>journal</gl-cor:entriesType>");
        assertThat(saved.getLedgerXml()).contains("<gl-cor:entriesType>ledger</gl-cor:entriesType>");
    }

    @Test
    void generate_queriesFullMonthDateRange() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, 2026, 2))
                .thenReturn(Optional.empty());
        when(journalEntryRepository
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(runRepository.save(any(EDefterRun.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generate(2026, 2);

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(journalEntryRepository)
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        eq(COMPANY_ID), startCaptor.capture(), endCaptor.capture());

        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 1));
        // 2026 artık yıl değil → Şubat 28 gün
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void generate_emptyEntries_savesZeroCounts() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.empty());
        when(journalEntryRepository
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        anyLong(), any(), any()))
                .thenReturn(List.of());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(runRepository.save(any(EDefterRun.class))).thenAnswer(inv -> inv.getArgument(0));

        EDefterRun result = service.generate(2026, 5);

        assertThat(result.getJournalEntryCount()).isZero();
        assertThat(result.getJournalLineCount()).isZero();
    }

    @Test
    void generate_whenRunAlreadyExists_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(COMPANY_ID, 2026, 3))
                .thenReturn(Optional.of(existingRun(55L, 2026, 3)));

        assertThatThrownBy(() -> service.generate(2026, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten üretildi")
                .hasMessageContaining("55");

        verify(runRepository, never()).save(any());
        verifyNoInteractions(journalEntryRepository, companyRepository);
    }

    // ── regenerate ──────────────────────────────────────────────────────────────

    @Test
    void regenerate_softDeletesOldRunAndGeneratesNewOne() {
        EDefterRun old = existingRun(99L, 2025, 12);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.of(old));
        when(journalEntryRepository
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        anyLong(), any(), any()))
                .thenReturn(List.of(sampleEntry()));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(runRepository.save(any(EDefterRun.class))).thenAnswer(inv -> inv.getArgument(0));

        EDefterRun result = service.regenerate(99L);

        // Eski kayıt soft-delete edilmiş olmalı
        assertThat(old.isDeleted()).isTrue();
        assertThat(old.getDeletedAt()).isNotNull();

        // İki kayıt save edilir: eski (soft delete) + yeni
        verify(runRepository, times(2)).save(any(EDefterRun.class));

        // Yeni kayıt eski kaydın yıl/ayını kullanmalı
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonth()).isEqualTo(12);
        assertThat(result.getStatus()).isEqualTo(EDefterStatus.GENERATED);
        assertThat(result.getJournalEntryCount()).isEqualTo(1);
    }

    @Test
    void regenerate_whenRunNotFound_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(404L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerate(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bulunamadı")
                .hasMessageContaining("404");

        verify(runRepository, never()).save(any());
    }

    // ── list / listByYear / getById ───────────────────────────────────────────

    @Test
    void list_returnsRepositoryResultForCurrentCompany() {
        List<EDefterRun> runs = List.of(existingRun(1L, 2026, 1), existingRun(2L, 2026, 2));
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(COMPANY_ID))
                .thenReturn(runs);

        assertThat(service.list()).isSameAs(runs);
    }

    @Test
    void listByYear_returnsRepositoryResultForGivenYear() {
        List<EDefterRun> runs = List.of(existingRun(1L, 2026, 1));
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByCompanyCompanyIdAndYearAndIsDeletedFalseOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(runs);

        assertThat(service.listByYear(2026)).isSameAs(runs);
    }

    @Test
    void getById_returnsRunWhenFound() {
        EDefterRun run = existingRun(5L, 2026, 4);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        assertThat(service.getById(5L)).isSameAs(run);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-Defter çalışması bulunamadı");
    }

    // ── getJournalXml / getLedgerXml ──────────────────────────────────────────

    @Test
    void getJournalXml_returnsStoredXml() {
        EDefterRun run = existingRun(5L, 2026, 4);
        run.setJournalXml("<journal/>");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        assertThat(service.getJournalXml(5L)).isEqualTo("<journal/>");
    }

    @Test
    void getJournalXml_throwsWhenXmlNull() {
        EDefterRun run = existingRun(5L, 2026, 4);
        run.setJournalXml(null);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getJournalXml(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yevmiye XML");
    }

    @Test
    void getLedgerXml_returnsStoredXml() {
        EDefterRun run = existingRun(5L, 2026, 4);
        run.setLedgerXml("<ledger/>");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        assertThat(service.getLedgerXml(5L)).isEqualTo("<ledger/>");
    }

    @Test
    void getLedgerXml_throwsWhenXmlNull() {
        EDefterRun run = existingRun(5L, 2026, 4);
        run.setLedgerXml(null);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getLedgerXml(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kebir XML");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_softDeletesRun() {
        EDefterRun run = existingRun(5L, 2026, 4);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(run));

        service.delete(5L);

        assertThat(run.isDeleted()).isTrue();
        assertThat(run.getDeletedAt()).isNotNull();
        verify(runRepository).save(run);
    }

    @Test
    void delete_throwsWhenRunNotFound() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(BusinessException.class);
        verify(runRepository, never()).save(any());
    }
}
