package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private AccountingPeriodGuard periodGuard;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private TransactionService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        account = new BankAccount();
        account.setAccountId(10L);
        account.setCompany(company);

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    // Tests successful income creation and journal entry generation.
    @Test
    void createTransaction_whenIncomeWithoutInvoice_savesAndCreatesJournalEntry() {
        TransactionRequestDto dto = request(TransactionType.INCOME, TransactionCategory.GENERAL, new BigDecimal("1250.00"));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            tx.setTransactionId(100L);
            return tx;
        });

        TransactionResponseDto result = service.createTransaction(dto);

        assertThat(result.transactionId()).isEqualTo(100L);
        assertThat(result.transactionType()).isEqualTo("INCOME");
        assertThat(result.transactionCategory()).isEqualTo("GENERAL");
        verify(journalEntryService).createForTransaction(any(Transaction.class));
    }

    // Tests that invoice-linked transactions skip duplicate journal creation.
    @Test
    void createTransaction_whenLinkedToInvoice_savesButDoesNotCreateJournalEntry() {
        TransactionRequestDto dto = new TransactionRequestDto(
                10L,
                null,
                55L,
                TransactionType.INCOME,
                TransactionCategory.GENERAL,
                new BigDecimal("500.00"),
                LocalDate.of(2026, 5, 20),
                "Fatura tahsilati",
                null,
                false
        );
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(55L, COMPANY_ID))
                .thenReturn(Optional.of(new Invoice()));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDto result = service.createTransaction(dto);

        assertThat(result.invoiceId()).isEqualTo(55L);
        verify(journalEntryService, never()).createForTransaction(any());
    }

    // Tests that closed accounting periods stop persistence before saving.
    @Test
    void createTransaction_whenPeriodIsClosed_throwsBeforeSaving() {
        TransactionRequestDto dto = request(TransactionType.INCOME, TransactionCategory.GENERAL, new BigDecimal("100.00"));
        doClosedPeriod(dto.transactionDate());

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(ClosedPeriodException.class);

        verify(transactionRepository, never()).save(any());
    }

    // Tests insufficient balance protection for cash or bank expense transactions.
    @Test
    void createTransaction_whenExpenseExceedsBalance_throwsAndDoesNotSave() {
        TransactionRequestDto dto = request(TransactionType.EXPENSE, TransactionCategory.GENERAL, new BigDecimal("900.00"));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bakiye yetersiz");

        verify(transactionRepository, never()).save(any());
        verify(journalEntryService, never()).createForTransaction(any());
    }

    // Tests transfer validation when target account is missing.
    @Test
    void createTransaction_whenTransferHasNoTargetAccount_throws() {
        TransactionRequestDto dto = new TransactionRequestDto(
                10L,
                null,
                null,
                TransactionType.INCOME,
                TransactionCategory.TRANSFER,
                new BigDecimal("250.00"),
                LocalDate.of(2026, 5, 20),
                "Virman",
                null,
                false
        );
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hedef hesap zorunludur");

        verify(transactionRepository, never()).save(any());
    }

    // Tests transfer transactions are saved as expense and receive the default transfer label.
    @Test
    void createTransaction_whenTransferIsValid_setsExpenseTypeAndTransferLabel() {
        BankAccount target = new BankAccount();
        target.setAccountId(20L);
        target.setCompany(company);
        TransactionRequestDto dto = new TransactionRequestDto(
                10L,
                20L,
                null,
                TransactionType.INCOME,
                TransactionCategory.TRANSFER,
                new BigDecimal("250.00"),
                LocalDate.of(2026, 5, 20),
                "Virman",
                null,
                false
        );
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(20L, COMPANY_ID))
                .thenReturn(Optional.of(target));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(new BigDecimal("1000.00"));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDto result = service.createTransaction(dto);

        assertThat(result.transactionType()).isEqualTo("EXPENSE");
        assertThat(result.transactionCategory()).isEqualTo("TRANSFER");
        assertThat(result.transferAccountId()).isEqualTo(20L);
        assertThat(result.category()).isEqualTo("Virman");
    }

    // Tests update balance control restores the old expense effect before comparing new amount.
    @Test
    void updateTransaction_whenExpenseAmountUsesRestoredOldBalance_updatesAndRecreatesJournal() {
        Transaction existing = transaction(77L, TransactionType.EXPENSE, new BigDecimal("100.00"));
        TransactionRequestDto dto = request(TransactionType.EXPENSE, TransactionCategory.GENERAL, new BigDecimal("120.00"));
        when(transactionRepository.findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(77L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(new BigDecimal("50.00"));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDto result = service.updateTransaction(77L, dto);

        assertThat(result.amount()).isEqualByComparingTo("120.00");
        verify(journalEntryService).reverseForTransaction(eq(COMPANY_ID), eq(77L), any());
        verify(journalEntryService).createForTransaction(existing);
    }

    // Tests that a reversed update is not created for invoice-owned transactions.
    @Test
    void updateTransaction_whenInvoiceLinked_doesNotReverseOrCreateJournal() {
        Transaction existing = transaction(77L, TransactionType.INCOME, new BigDecimal("100.00"));
        existing.setInvoiceId(55L);
        TransactionRequestDto dto = new TransactionRequestDto(
                10L,
                null,
                55L,
                TransactionType.INCOME,
                TransactionCategory.GENERAL,
                new BigDecimal("150.00"),
                LocalDate.of(2026, 5, 21),
                "Fatura tahsilati",
                null,
                false
        );
        when(transactionRepository.findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(77L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(55L, COMPANY_ID))
                .thenReturn(Optional.of(new Invoice()));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateTransaction(77L, dto);

        verify(journalEntryService, never()).reverseForTransaction(anyLong(), anyLong(), any());
        verify(journalEntryService, never()).createForTransaction(any());
    }

    // Tests date range validation before hitting the repository.
    @Test
    void getTransactionsByDateRange_whenStartAfterEnd_throws() {
        assertThatThrownBy(() -> service.getTransactionsByDateRange(
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tarihi");
    }

    // Tests account query validates ownership and maps repository rows.
    @Test
    void getTransactionsByAccount_whenAccountExists_returnsMappedRows() {
        Transaction tx = transaction(90L, TransactionType.INCOME, new BigDecimal("75.00"));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(10L, COMPANY_ID))
                .thenReturn(List.of(tx));

        List<TransactionResponseDto> result = service.getTransactionsByAccount(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transactionId()).isEqualTo(90L);
    }

    // Tests soft delete marks the transaction and reverses only standalone journal entries.
    @Test
    void softDeleteTransaction_whenStandalone_marksDeletedAndReversesJournal() {
        Transaction tx = transaction(88L, TransactionType.INCOME, new BigDecimal("400.00"));
        when(transactionRepository.findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(88L, COMPANY_ID))
                .thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDeleteTransaction(88L);

        assertThat(tx.isDeleted()).isTrue();
        assertThat(tx.getDeletedAt()).isNotNull();
        verify(journalEntryService).reverseForTransaction(eq(COMPANY_ID), eq(88L), any());
    }

    // Tests restore clears soft-delete markers and returns the restored DTO.
    @Test
    void restoreTransaction_whenDeleted_clearsDeletedFlags() {
        Transaction tx = transaction(88L, TransactionType.INCOME, new BigDecimal("400.00"));
        tx.setDeleted(true);
        tx.setDeletedAt(LocalDateTime.of(2026, 5, 20, 12, 0));
        when(transactionRepository.findByTransactionIdAndCompanyCompanyId(88L, COMPANY_ID))
                .thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponseDto result = service.restoreTransaction(88L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(tx.getDeletedAt()).isNull();
    }

    // Tests expired soft-deleted transactions are permanently deleted.
    @Test
    void hardDeleteExpired_whenExpiredTransactionsExist_deletesAndReturnsCount() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        Transaction first = transaction(1L, TransactionType.INCOME, BigDecimal.TEN);
        Transaction second = transaction(2L, TransactionType.EXPENSE, BigDecimal.ONE);
        when(transactionRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(transactionRepository).delete(first);
        verify(transactionRepository).delete(second);
    }

    private TransactionRequestDto request(TransactionType type, TransactionCategory category, BigDecimal amount) {
        return new TransactionRequestDto(
                10L,
                null,
                null,
                type,
                category,
                amount,
                LocalDate.of(2026, 5, 20),
                "Test islem",
                null,
                false
        );
    }

    private Transaction transaction(Long id, TransactionType type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTransactionId(id);
        tx.setCompany(company);
        tx.setAccountId(10L);
        tx.setTransactionType(type);
        tx.setTransactionCategory(TransactionCategory.GENERAL);
        tx.setAmount(amount);
        tx.setTransactionDate(LocalDate.of(2026, 5, 20));
        tx.setDescription("Kayit");
        tx.setDeleted(false);
        return tx;
    }

    private void doClosedPeriod(LocalDate date) {
        org.mockito.Mockito.doThrow(new ClosedPeriodException(date.getYear(), date.getMonthValue()))
                .when(periodGuard).assertOpen(date);
    }
}
