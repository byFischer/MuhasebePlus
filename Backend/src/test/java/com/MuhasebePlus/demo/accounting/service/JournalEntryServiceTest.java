package com.MuhasebePlus.demo.accounting.service;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryLineRequestDto;
import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.entity.*;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntrySequenceRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock private JournalEntryRepository entryRepository;
    @Mock private JournalEntryLineRepository lineRepository;
    @Mock private JournalEntrySequenceRepository sequenceRepository;
    @Mock private ChartOfAccountService chartOfAccountService;
    @Mock private CustomerRepository customerRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private JournalEntryService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");
    }

    // ── createManualEntry ─────────────────────────────────────────────────────

    @Test
    void createManualEntry_whenAccountingNotSetup_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(false);

        JournalEntryRequestDto dto = balancedDto();

        assertThatThrownBy(() -> service.createManualEntry(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TDHP");

        verify(entryRepository, never()).save(any());
    }

    @Test
    void createManualEntry_whenLinesUnbalanced_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        JournalEntryRequestDto dto = unbalancedDto();

        assertThatThrownBy(() -> service.createManualEntry(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("borç")
                .hasMessageContaining("alacak");

        verify(entryRepository, never()).save(any());
    }

    @Test
    void createManualEntry_whenBalanced_savesAndReturnsDto() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            e.setEntryId(100L);
            return e;
        });

        JournalEntryRequestDto dto = balancedDto();
        JournalEntryResponseDto result = service.createManualEntry(dto);

        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo(JournalSourceType.MANUAL);
        verify(entryRepository).save(argThat(e -> e.getSourceType() == JournalSourceType.MANUAL));
    }

    @Test
    void createManualEntry_entryNumber_followsFisFormat() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createManualEntry(balancedDto());

        verify(entryRepository).save(argThat(e ->
                e.getEntryNumber().matches("FIS-\\d{4}-\\d{4}")
        ));
    }

    // ── reverseEntry ──────────────────────────────────────────────────────────

    @Test
    void reverseEntry_whenAlreadyReversed_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        JournalEntry entry = entryWithLines(JournalSourceType.MANUAL);
        entry.setReversed(true);
        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 1L))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.reverseEntry(1L, "İptal"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten ters çevrilmiş");
    }

    @Test
    void reverseEntry_whenValid_createsReversalWithSwappedAmounts() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        JournalEntry original = entryWithLines(JournalSourceType.MANUAL);
        original.setEntryId(10L);
        original.setEntryNumber("FIS-2026-0001");
        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 10L))
                .thenReturn(Optional.of(original));
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            if (e.getEntryId() == null) e.setEntryId(11L);
            return e;
        });

        service.reverseEntry(10L, "Test iptali");

        // save çağrıları: reversal kaydı + original'i güncellemek
        verify(entryRepository, times(2)).save(any());
        assertThat(original.isReversed()).isTrue();
    }

    @Test
    void reverseEntry_reversalLines_haveSwappedDebitCredit() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        JournalEntry original = entryWithLines(JournalSourceType.MANUAL);
        original.setEntryId(10L);
        original.setEntryNumber("FIS-2026-0001");

        // Orijinal satır: borç=1000, alacak=0
        JournalEntryLine line = original.getLines().get(0);
        BigDecimal originalDebit = line.getDebitAmount();
        BigDecimal originalCredit = line.getCreditAmount();

        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 10L))
                .thenReturn(Optional.of(original));
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());

        List<JournalEntry> savedEntries = new ArrayList<>();
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            if (e.getEntryId() == null) e.setEntryId(11L);
            savedEntries.add(e);
            return e;
        });

        service.reverseEntry(10L, null);

        JournalEntry reversal = savedEntries.stream()
                .filter(e -> e.getSourceType() == JournalSourceType.REVERSAL)
                .findFirst().orElseThrow();

        JournalEntryLine reversalLine = reversal.getLines().get(0);
        assertThat(reversalLine.getDebitAmount()).isEqualByComparingTo(originalCredit);
        assertThat(reversalLine.getCreditAmount()).isEqualByComparingTo(originalDebit);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_whenSourceTypeIsInvoice_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        JournalEntry entry = entryWithLines(JournalSourceType.INVOICE);
        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 1L))
                .thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sadece manuel fişler");
    }

    @Test
    void delete_whenSourceTypeIsManual_softDeletesEntry() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        JournalEntry entry = entryWithLines(JournalSourceType.MANUAL);
        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 1L))
                .thenReturn(Optional.of(entry));

        service.delete(1L);

        assertThat(entry.isDeleted()).isTrue();
        assertThat(entry.getDeletedAt()).isNotNull();
        verify(entryRepository).save(entry);
    }

    @Test
    void delete_whenEntryNotFound_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(entryRepository.findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(COMPANY_ID, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fiş bulunamadı");
    }

    // ── createForInvoice ──────────────────────────────────────────────────────

    @Test
    void createForInvoice_whenAccountingNotSetup_returnsWithoutSaving() {
        var invoice = invoiceStub(COMPANY_ID, 5L);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(false);

        service.createForInvoice(invoice);

        verify(entryRepository, never()).save(any());
    }

    @Test
    void createForInvoice_whenEntryAlreadyExists_returnsWithoutSaving() {
        var invoice = invoiceStub(COMPANY_ID, 5L);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.INVOICE, 5L)).thenReturn(true);

        service.createForInvoice(invoice);

        verify(entryRepository, never()).save(any());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private JournalEntryRequestDto balancedDto() {
        return new JournalEntryRequestDto(
                LocalDate.now(),
                "Test dengeli fişi",
                List.of(
                        new JournalEntryLineRequestDto(100L, new BigDecimal("1000.00"), BigDecimal.ZERO, "Borç satırı"),
                        new JournalEntryLineRequestDto(200L, BigDecimal.ZERO, new BigDecimal("1000.00"), "Alacak satırı")
                )
        );
    }

    private JournalEntryRequestDto unbalancedDto() {
        return new JournalEntryRequestDto(
                LocalDate.now(),
                "Dengesiz fiş",
                List.of(
                        new JournalEntryLineRequestDto(100L, new BigDecimal("999.00"), BigDecimal.ZERO, "Sadece borç")
                )
        );
    }

    private JournalEntry entryWithLines(JournalSourceType sourceType) {
        JournalEntry entry = new JournalEntry();
        entry.setCompany(company);
        entry.setEntryDate(LocalDate.now());
        entry.setDescription("Test fişi");
        entry.setSourceType(sourceType);
        entry.setReversed(false);

        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setCompany(company);
        debitLine.setAccountId(100L);
        debitLine.setDebitAmount(new BigDecimal("1000.00"));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setDescription("Borç");
        debitLine.setLineOrder(0);

        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setCompany(company);
        creditLine.setAccountId(200L);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal("1000.00"));
        creditLine.setDescription("Alacak");
        creditLine.setLineOrder(1);

        entry.setLines(new ArrayList<>(List.of(debitLine, creditLine)));
        return entry;
    }

    private com.MuhasebePlus.demo.invoice.entity.Invoice invoiceStub(Long companyId, Long invoiceId) {
        var invoice = new com.MuhasebePlus.demo.invoice.entity.Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompany(company);
        invoice.setInvoiceType(com.MuhasebePlus.demo.invoice.entity.InvoiceType.sale);
        invoice.setSubtotal(new BigDecimal("1000.00"));
        invoice.setVatAmount(new BigDecimal("180.00"));
        invoice.setTotalAmount(new BigDecimal("1180.00"));
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setInvoiceNumber("FT-2026-0001");
        return invoice;
    }
}
