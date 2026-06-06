package com.MuhasebePlus.demo.accounting.service;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryLineRequestDto;
import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.entity.*;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntrySequenceRepository;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import com.MuhasebePlus.demo.fixedasset.entity.DepreciationLine;
import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
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
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CompanyContext companyContext;
    @Mock private AccountingPeriodGuard periodGuard;

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
    void createManualEntry_whenPeriodIsClosed_throwsBeforeSaving() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        LocalDate entryDate = LocalDate.of(2026, 5, 31);
        JournalEntryRequestDto dto = new JournalEntryRequestDto(
                entryDate,
                "Kapali donem fisi",
                balancedDto().lines()
        );
        doThrow(new ClosedPeriodException(2026, 5)).when(periodGuard).assertOpen(entryDate);

        assertThatThrownBy(() -> service.createManualEntry(dto))
                .isInstanceOf(ClosedPeriodException.class)
                .hasMessageContaining("2026")
                .hasMessageContaining("05");

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

    @Test
    void createForInvoice_saleWithProductCost_addsCostOfGoodsSoldLines() {
        var invoice = invoiceStub(COMPANY_ID, 5L);
        InvoiceLineItem line = new InvoiceLineItem();
        line.setProductId(10);
        line.setQuantity(2);
        Product product = Product.builder()
                .productId(10)
                .company(company)
                .costPrice(new BigDecimal("250.00"))
                .build();

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.INVOICE, 5L)).thenReturn(false);
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(List.of(line));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(10, COMPANY_ID))
                .thenReturn(Optional.of(product));
        stubAccount("120", 120L);
        stubAccount("600", 600L);
        stubAccount("391", 391L);
        stubAccount("621", 621L);
        stubAccount("153", 153L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForInvoice(invoice);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.INVOICE
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(621L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("500.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(153L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("500.00")) == 0)
        ));
    }

    @Test
    void createForDepreciation_createsBalancedDepreciationEntry() {
        AssetCategory category = AssetCategory.builder()
                .name("Demirbas")
                .depreciationAccountCode("257")
                .build();
        FixedAsset asset = FixedAsset.builder()
                .company(company)
                .category(category)
                .name("Laptop")
                .build();
        DepreciationLine line = DepreciationLine.builder()
                .id(55L)
                .fixedAsset(asset)
                .periodYear(2026)
                .periodMonth(5)
                .depreciationAmount(new BigDecimal("100.00"))
                .build();

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.DEPRECIATION, 55L)).thenReturn(false);
        stubAccount("770", 770L);
        stubAccount("257", 257L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            e.setEntryId(200L);
            return e;
        });

        Long entryId = service.createForDepreciation(line);

        assertThat(entryId).isEqualTo(200L);
        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.DEPRECIATION
                        && entry.getSourceId().equals(55L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(770L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("100.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(257L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("100.00")) == 0)
        ));
    }

    @Test
    void createForVatSettlement_whenPayable_closesVatAccountsAndCreditsTaxPayable() {
        VatPeriod vatPeriod = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .totalOutputVat(new BigDecimal("120.00"))
                .totalInputVat(new BigDecimal("50.00"))
                .previousCarriedForwardVat(new BigDecimal("20.00"))
                .netPayableVat(new BigDecimal("50.00"))
                .carriedForwardVat(BigDecimal.ZERO)
                .build();
        vatPeriod.setId(500L);

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.VAT_DECLARATION, 500L)).thenReturn(false);
        stubAccount("391", 391L);
        stubAccount("191", 191L);
        stubAccount("190", 190L);
        stubAccount("360", 360L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            e.setEntryId(501L);
            return e;
        });

        Long entryId = service.createForVatSettlement(vatPeriod);

        assertThat(entryId).isEqualTo(501L);
        verify(periodGuard).assertOpen(LocalDate.of(2026, 5, 31));
        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.VAT_DECLARATION
                        && entry.getSourceId().equals(500L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(391L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("120.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(191L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("50.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(190L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("20.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(360L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("50.00")) == 0)
        ));
    }

    @Test
    void createForChequePayment_saleInvoice_debitsReceivedChequesAndCreditsCustomer() {
        Invoice invoice = invoiceStub(COMPANY_ID, 5L);
        InvoicePayment payment = InvoicePayment.builder()
                .paymentId(88L)
                .company(company)
                .invoiceId(5L)
                .amount(new BigDecimal("750.00"))
                .paymentDate(LocalDate.of(2026, 5, 12))
                .build();
        Cheque cheque = Cheque.builder()
                .chequeId(70L)
                .company(company)
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("750.00"))
                .build();

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.PAYMENT, 88L)).thenReturn(false);
        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(invoice));
        stubAccount("101", 101L);
        stubAccount("120", 120L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForChequePayment(payment, cheque);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.PAYMENT
                        && entry.getSourceId().equals(88L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(101L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("750.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(120L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("750.00")) == 0)
        ));
    }

    @Test
    void createForChequePayment_purchaseInvoice_debitsVendorAndCreditsIssuedCheques() {
        Invoice invoice = invoiceStub(COMPANY_ID, 6L);
        invoice.setInvoiceType(InvoiceType.purchase);
        InvoicePayment payment = InvoicePayment.builder()
                .paymentId(89L)
                .company(company)
                .invoiceId(6L)
                .amount(new BigDecimal("900.00"))
                .paymentDate(LocalDate.of(2026, 5, 13))
                .build();
        Cheque cheque = Cheque.builder()
                .chequeId(71L)
                .company(company)
                .chequeType(ChequeType.PAYABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("900.00"))
                .build();

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.PAYMENT, 89L)).thenReturn(false);
        when(invoiceRepository.findById(6L)).thenReturn(Optional.of(invoice));
        stubAccount("320", 320L);
        stubAccount("103", 103L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForChequePayment(payment, cheque);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.PAYMENT
                        && entry.getSourceId().equals(89L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(320L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("900.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(103L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("900.00")) == 0)
        ));
    }

    @Test
    void createForChequeSettlement_receivableCheque_debitsBankAndCreditsChequePortfolio() {
        Cheque cheque = Cheque.builder()
                .chequeId(72L)
                .company(company)
                .chequeNumber("CHK-72")
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("650.00"))
                .build();
        Transaction transaction = new Transaction();
        transaction.setAccountId(10L);
        transaction.setTransactionDate(LocalDate.of(2026, 5, 14));

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.CHEQUE, 72L)).thenReturn(false);
        stubAccount("102", 102L);
        stubAccount("101", 101L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForChequeSettlement(cheque, transaction);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.CHEQUE
                        && entry.getSourceId().equals(72L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(102L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("650.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(101L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("650.00")) == 0)
        ));
    }

    @Test
    void createForTransaction_cashIncome_debitsCashAccount100() {
        Transaction tx = new Transaction();
        tx.setTransactionId(300L);
        tx.setCompany(company);
        tx.setAccountId(10L);
        tx.setTransactionType(TransactionType.INCOME);
        tx.setTransactionCategory(TransactionCategory.GENERAL);
        tx.setAmount(new BigDecimal("120.00"));
        tx.setTransactionDate(LocalDate.of(2026, 5, 15));
        tx.setDescription("Kasa tahsilati");

        BankAccount cash = new BankAccount();
        cash.setAccountId(10L);
        cash.setAccountType(BankAccountType.CASH);

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.TRANSACTION, 300L)).thenReturn(false);
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(cash));
        stubAccount("100", 100L);
        stubAccount("602", 602L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForTransaction(tx);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.TRANSACTION
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(100L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("120.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(602L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("120.00")) == 0)
        ));
    }

    @Test
    void createForTransaction_bankFee_debitsBankFeeExpense653() {
        Transaction tx = new Transaction();
        tx.setTransactionId(301L);
        tx.setCompany(company);
        tx.setAccountId(10L);
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionCategory(TransactionCategory.BANK_FEE);
        tx.setAmount(new BigDecimal("45.00"));
        tx.setTransactionDate(LocalDate.of(2026, 5, 16));
        tx.setDescription("EFT masrafi");

        BankAccount bank = new BankAccount();
        bank.setAccountId(10L);
        bank.setAccountType(BankAccountType.BANK);

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.TRANSACTION, 301L)).thenReturn(false);
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(bank));
        stubAccount("653", 653L);
        stubAccount("102", 102L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForTransaction(tx);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.TRANSACTION
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(653L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("45.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(102L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("45.00")) == 0)
        ));
    }

    @Test
    void createForTransaction_transfer_debitsTargetAndCreditsSource() {
        Transaction tx = new Transaction();
        tx.setTransactionId(302L);
        tx.setCompany(company);
        tx.setAccountId(10L);
        tx.setTransferAccountId(20L);
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionCategory(TransactionCategory.TRANSFER);
        tx.setAmount(new BigDecimal("800.00"));
        tx.setTransactionDate(LocalDate.of(2026, 5, 17));
        tx.setDescription("Bankadan kasaya virman");

        BankAccount bank = new BankAccount();
        bank.setAccountId(10L);
        bank.setAccountType(BankAccountType.BANK);
        BankAccount cash = new BankAccount();
        cash.setAccountId(20L);
        cash.setAccountType(BankAccountType.CASH);

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.TRANSACTION, 302L)).thenReturn(false);
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(bank));
        when(bankAccountRepository.findById(20L)).thenReturn(Optional.of(cash));
        stubAccount("100", 100L);
        stubAccount("102", 102L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForTransaction(tx);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.TRANSACTION
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(100L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("800.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(102L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("800.00")) == 0)
        ));
    }

    @Test
    void createForCustomerOpening_positiveBuyer_debitsCustomerAndCreditsOpeningAccount() {
        Customer customer = new Customer();
        customer.setCustomerId(44L);
        customer.setCompany(company);
        customer.setName("ABC Musteri");
        customer.setCustomerRole(CustomerRole.BUYER);
        customer.setAccountCode("120.01");
        customer.setOpeningBalance(new BigDecimal("1250.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.CUSTOMER_OPENING, 44L)).thenReturn(false);
        stubAccount("120.01", 12001L);
        stubAccount("570", 570L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            e.setEntryId(300L);
            return e;
        });

        Long entryId = service.createForCustomerOpening(customer);

        assertThat(entryId).isEqualTo(300L);
        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.CUSTOMER_OPENING
                        && entry.getSourceId().equals(44L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(12001L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("1250.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(570L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("1250.00")) == 0)
        ));
    }

    @Test
    void createForCustomerOpening_negativeSeller_debitsOpeningAndCreditsVendorAccount() {
        Customer customer = new Customer();
        customer.setCustomerId(45L);
        customer.setCompany(company);
        customer.setName("XYZ Tedarikci");
        customer.setCustomerRole(CustomerRole.SELLER);
        customer.setOpeningBalance(new BigDecimal("-900.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));

        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                COMPANY_ID, JournalSourceType.CUSTOMER_OPENING, 45L)).thenReturn(false);
        stubAccount("570", 570L);
        stubAccount("320", 320L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createForCustomerOpening(customer);

        verify(entryRepository).save(argThat(entry ->
                entry.getSourceType() == JournalSourceType.CUSTOMER_OPENING
                        && entry.getSourceId().equals(45L)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(570L)
                                        && l.getDebitAmount().compareTo(new BigDecimal("900.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(320L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("900.00")) == 0)
        ));
    }

    @Test
    void getIncomeStatement_calculatesIncomeExpenseAndNetProfit() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(lineRepository.sumByAccountForPeriod(COMPANY_ID, start, end))
                .thenReturn(List.<Object[]>of(
                        row(600L, "0.00", "1000.00"),
                        row(621L, "400.00", "0.00"),
                        row(770L, "100.00", "0.00")
                ));
        stubAccountById(600L, "600", "Yurtici Satislar", AccountType.INCOME);
        stubAccountById(621L, "621", "Satilan Ticari Mallar Maliyeti", AccountType.COST);
        stubAccountById(770L, "770", "Genel Yonetim Giderleri", AccountType.COST);

        IncomeStatementResponseDto result = service.getIncomeStatement(start, end);

        assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.netProfit()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.profitMarginPercent()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.sections()).hasSize(2);
    }

    @Test
    void getBalanceSheet_calculatesCumulativeAssetsLiabilitiesAndEquity() {
        LocalDate asOfDate = LocalDate.of(2026, 12, 31);

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(lineRepository.sumByAccountUntil(COMPANY_ID, asOfDate))
                .thenReturn(List.<Object[]>of(
                        row(102L, "2500.00", "0.00"),
                        row(320L, "0.00", "600.00"),
                        row(500L, "0.00", "1400.00")
                ));
        when(lineRepository.sumByAccountForPeriod(COMPANY_ID, LocalDate.of(2026, 1, 1), asOfDate))
                .thenReturn(List.<Object[]>of(
                        row(600L, "0.00", "1000.00"),
                        row(621L, "500.00", "0.00")
                ));
        stubAccountById(102L, "102", "Bankalar", AccountType.ASSET);
        stubAccountById(320L, "320", "Saticilar", AccountType.LIABILITY);
        stubAccountById(500L, "500", "Sermaye", AccountType.EQUITY);
        stubAccountById(600L, "600", "Yurtici Satislar", AccountType.INCOME);
        stubAccountById(621L, "621", "Satilan Ticari Mallar Maliyeti", AccountType.COST);

        BalanceSheetResponseDto result = service.getBalanceSheet(asOfDate);

        assertThat(result.totalAssets()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.totalLiabilities()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(result.totalEquity()).isEqualByComparingTo(new BigDecimal("1900.00"));
        assertThat(result.totalLiabilitiesAndEquity()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.difference()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.assetSections()).extracting("sectionCode").contains("1");
        assertThat(result.equitySections().get(0).lines())
                .anyMatch(line -> line.accountCode().equals("590")
                        && line.amount().compareTo(new BigDecimal("500.00")) == 0);
        verify(lineRepository).sumByAccountUntil(COMPANY_ID, asOfDate);
    }

    @Test
    void createYearEndClosingEntry_closesIncomeAndCostAccountsByAccountType() {
        when(lineRepository.sumByAccountForPeriod(
                COMPANY_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)))
                .thenReturn(List.<Object[]>of(
                        row(600L, "0.00", "1000.00"),
                        row(621L, "400.00", "0.00")
                ));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        stubAccountById(600L, "600", "Yurtici Satislar", AccountType.INCOME);
        stubAccountById(621L, "621", "Satilan Ticari Mallar Maliyeti", AccountType.COST);
        stubAccount("590", 590L);
        when(sequenceRepository.findByCompanyIdAndYearForUpdate(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(inv -> {
            JournalEntry entry = inv.getArgument(0);
            entry.setEntryId(5900L);
            return entry;
        });

        Long entryId = service.createYearEndClosingEntry(COMPANY_ID, 2026);

        assertThat(entryId).isEqualTo(5900L);
        verify(entryRepository).save(argThat(entry ->
                entry.getLines().stream().anyMatch(l ->
                        l.getAccountId().equals(600L)
                                && l.getDebitAmount().compareTo(new BigDecimal("1000.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(621L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("400.00")) == 0)
                        && entry.getLines().stream().anyMatch(l ->
                                l.getAccountId().equals(590L)
                                        && l.getCreditAmount().compareTo(new BigDecimal("600.00")) == 0)
        ));
    }

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

    private void stubAccount(String code, Long accountId) {
        ChartOfAccount account = new ChartOfAccount();
        account.setAccountId(accountId);
        account.setAccountCode(code);
        account.setAccountName("Hesap " + code);
        account.setAccountType(AccountType.ASSET);
        when(chartOfAccountService.findByCode(COMPANY_ID, code)).thenReturn(Optional.of(account));
    }

    private void stubAccountById(Long accountId, String code, String name, AccountType type) {
        ChartOfAccount account = new ChartOfAccount();
        account.setAccountId(accountId);
        account.setAccountCode(code);
        account.setAccountName(name);
        account.setAccountType(type);
        when(chartOfAccountService.findByAccountId(accountId)).thenReturn(Optional.of(account));
    }

    private Object[] row(Long accountId, String debit, String credit) {
        return new Object[] { accountId, new BigDecimal(debit), new BigDecimal(credit) };
    }
}
