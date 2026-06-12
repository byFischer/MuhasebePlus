package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.ChartOfAccountService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.BankAccountRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankAccountResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankAccountServiceTest {

    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private ChartOfAccountService chartOfAccountService;

    @InjectMocks
    private BankAccountService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 7L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
    }

    // Tests bank account creation with IBAN validation and 102 account code generation.
    @Test
    void createBankAccount_whenBankAccountIsValid_savesWithBankParentAccountCode() {
        BankAccountRequestDto dto = request("Test Bank", "TR123456789012345678901234", BankAccountType.BANK);
        when(bankAccountRepository.existsByIbanAndCompanyCompanyIdAndIsDeletedFalse(dto.iban(), COMPANY_ID))
                .thenReturn(false);
        when(chartOfAccountService.getOrCreateLeafAccount(COMPANY_ID, "102", "Test Bank", AccountType.ASSET))
                .thenReturn("102.001");
        when(bankAccountRepository.save(any())).thenAnswer(inv -> {
            BankAccount account = inv.getArgument(0);
            account.setAccountId(10L);
            return account;
        });

        BankAccountResponseDto result = service.createBankAccount(dto);

        assertThat(result.accountId()).isEqualTo(10L);
        assertThat(result.iban()).isEqualTo(dto.iban());
        assertThat(result.accountType()).isEqualTo("BANK");
        assertThat(result.accountCode()).isEqualTo("102.001");
        assertThat(result.currentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Tests cash account creation ignores IBAN and uses 100 parent account.
    @Test
    void createBankAccount_whenCashAccount_ignoresIbanAndUsesCashParentCode() {
        BankAccountRequestDto dto = request("Merkez Kasa", "TR123456789012345678901234", BankAccountType.CASH);
        when(chartOfAccountService.getOrCreateLeafAccount(COMPANY_ID, "100", "Merkez Kasa", AccountType.ASSET))
                .thenReturn("100.001");
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankAccountResponseDto result = service.createBankAccount(dto);

        assertThat(result.iban()).isNull();
        assertThat(result.accountType()).isEqualTo("CASH");
        assertThat(result.accountCode()).isEqualTo("100.001");
        verify(bankAccountRepository, never()).existsByIbanAndCompanyCompanyIdAndIsDeletedFalse(any(), eq(COMPANY_ID));
    }

    // Tests duplicate IBAN protection before saving a bank account.
    @Test
    void createBankAccount_whenIbanAlreadyExists_throwsAndDoesNotSave() {
        BankAccountRequestDto dto = request("Test Bank", "TR123456789012345678901234", BankAccountType.BANK);
        when(bankAccountRepository.existsByIbanAndCompanyCompanyIdAndIsDeletedFalse(dto.iban(), COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createBankAccount(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("IBAN");

        verify(bankAccountRepository, never()).save(any());
    }

    // Tests invalid Turkish IBAN format is rejected before repository access.
    @Test
    void createBankAccount_whenIbanFormatIsInvalid_throwsAndDoesNotSave() {
        BankAccountRequestDto dto = request("Test Bank", "TR123", BankAccountType.BANK);

        assertThatThrownBy(() -> service.createBankAccount(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("IBAN");

        verify(bankAccountRepository, never()).save(any());
    }

    // Tests list balances include income, expense, and target-side transfer effects.
    @Test
    void getAllBankAccounts_calculatesBalancesIncludingTransferTargets() {
        BankAccount bank = account(10L, "Banka", BankAccountType.BANK, "102.001");
        BankAccount cash = account(20L, "Kasa", BankAccountType.CASH, "100.001");
        Transaction income = tx(10L, null, TransactionType.INCOME, new BigDecimal("100.00"));
        Transaction expense = tx(10L, null, TransactionType.EXPENSE, new BigDecimal("30.00"));
        Transaction transfer = tx(10L, 20L, TransactionType.EXPENSE, new BigDecimal("50.00"));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(bank, cash));
        when(transactionRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(COMPANY_ID))
                .thenReturn(List.of(income, expense, transfer));

        List<BankAccountResponseDto> result = service.getAllBankAccounts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).currentBalance()).isEqualByComparingTo("20.00");
        assertThat(result.get(1).currentBalance()).isEqualByComparingTo("50.00");
    }

    // Tests null repository balance is mapped as zero.
    @Test
    void getBankAccountById_whenBalanceIsNull_returnsZeroBalance() {
        BankAccount bank = account(10L, "Banka", BankAccountType.BANK, "102.001");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(null);

        BankAccountResponseDto result = service.getBankAccountById(10L);

        assertThat(result.currentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Tests update rejects duplicate IBAN owned by another account.
    @Test
    void updateBankAccount_whenNewIbanBelongsToAnotherAccount_throws() {
        BankAccount bank = account(10L, "Eski Banka", BankAccountType.BANK, "102.001");
        BankAccountRequestDto dto = request("Yeni Banka", "TR999999999999999999999999", BankAccountType.BANK);
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(bankAccountRepository.existsByIbanAndAccountIdNotAndCompanyCompanyIdAndIsDeletedFalse(
                dto.iban(), 10L, COMPANY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.updateBankAccount(10L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("IBAN");

        verify(bankAccountRepository, never()).save(any());
    }

    // Tests changing account type regenerates the TDHP account code when parent code changes.
    @Test
    void updateBankAccount_whenTypeChangesToCash_regeneratesAccountCodeAndClearsIban() {
        BankAccount bank = account(10L, "Eski Banka", BankAccountType.BANK, "102.001");
        BankAccountRequestDto dto = request("Merkez Kasa", "TR999999999999999999999999", BankAccountType.CASH);
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(chartOfAccountService.getOrCreateLeafAccount(COMPANY_ID, "100", "Merkez Kasa", AccountType.ASSET))
                .thenReturn("100.005");
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(new BigDecimal("25.00"));

        BankAccountResponseDto result = service.updateBankAccount(10L, dto);

        assertThat(result.iban()).isNull();
        assertThat(result.accountType()).isEqualTo("CASH");
        assertThat(result.accountCode()).isEqualTo("100.005");
        assertThat(result.currentBalance()).isEqualByComparingTo("25.00");
    }

    // Tests active transactions block account soft deletion.
    @Test
    void softDeleteBankAccount_whenActiveTransactionsExist_throwsAndDoesNotDelete() {
        BankAccount bank = account(10L, "Banka", BankAccountType.BANK, "102.001");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(transactionRepository.existsByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.softDeleteBankAccount(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("silinemez");

        assertThat(bank.isDeleted()).isFalse();
        verify(bankAccountRepository, never()).save(any());
    }

    // Tests account soft delete marks deletion metadata when no active transactions exist.
    @Test
    void softDeleteBankAccount_whenNoActiveTransactions_marksDeleted() {
        BankAccount bank = account(10L, "Banka", BankAccountType.BANK, "102.001");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(transactionRepository.existsByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(false);
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDeleteBankAccount(10L);

        assertThat(bank.isDeleted()).isTrue();
        assertThat(bank.getDeletedAt()).isNotNull();
    }

    // Tests restore clears soft-delete metadata and reports current balance.
    @Test
    void restoreBankAccount_whenDeleted_clearsDeletionMetadata() {
        BankAccount bank = account(10L, "Banka", BankAccountType.BANK, "102.001");
        bank.setDeleted(true);
        bank.setDeletedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(bank));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.calculateBalanceForAccount(10L, COMPANY_ID, TransactionType.INCOME))
                .thenReturn(new BigDecimal("12.50"));

        BankAccountResponseDto result = service.restoreBankAccount(10L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(result.currentBalance()).isEqualByComparingTo("12.50");
        assertThat(bank.getDeletedAt()).isNull();
    }

    // Tests expired soft-deleted accounts are permanently deleted.
    @Test
    void hardDeleteExpired_whenExpiredAccountsExist_deletesAndReturnsCount() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        BankAccount first = account(1L, "Banka 1", BankAccountType.BANK, "102.001");
        BankAccount second = account(2L, "Banka 2", BankAccountType.CASH, "100.001");
        when(bankAccountRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(bankAccountRepository).delete(first);
        verify(bankAccountRepository).delete(second);
    }

    private BankAccountRequestDto request(String bankName, String iban, BankAccountType accountType) {
        return new BankAccountRequestDto(bankName, iban, accountType, Currency.TRY);
    }

    private BankAccount account(Long id, String bankName, BankAccountType type, String accountCode) {
        BankAccount account = new BankAccount();
        account.setAccountId(id);
        account.setCompany(company);
        account.setBankName(bankName);
        account.setIban(type == BankAccountType.BANK ? "TR123456789012345678901234" : null);
        account.setAccountType(type);
        account.setCurrency(Currency.TRY);
        account.setAccountCode(accountCode);
        account.setDeleted(false);
        return account;
    }

    private Transaction tx(Long accountId, Long transferAccountId, TransactionType type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setTransferAccountId(transferAccountId);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        return tx;
    }
}
