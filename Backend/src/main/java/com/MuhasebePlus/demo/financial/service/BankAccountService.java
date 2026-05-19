package com.MuhasebePlus.demo.financial.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.BankAccountRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankAccountResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;

@Service
@Transactional
@RequiredArgsConstructor
public class BankAccountService implements HardDeletable {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final SystemLogService systemLogService;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;


    // PUBLIC METOTLAR

    public BankAccountResponseDto createBankAccount(BankAccountRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        if (bankAccountRepository.existsByIbanAndCompanyCompanyIdAndIsDeletedFalse(dto.iban(), companyId)) {
            throw new RuntimeException("Bu IBAN zaten kayıtlı: " + dto.iban());
        }

        BankAccount account = new BankAccount();
        account.setCompany(companyRepository.getReferenceById(companyId));
        account.setUserId(userId);
        account.setBankName(dto.bankName());
        account.setIban(dto.iban());
        account.setCurrency(dto.currency());
        account.setDeleted(false);

        BankAccount saved = bankAccountRepository.save(account);
        systemLogService.log(LogLevel.INFO, "Banka hesabı oluşturuldu: " + saved.getBankName() + " - " + saved.getIban());
        return toResponseDto(saved, BigDecimal.ZERO);
    }

    public List<BankAccountResponseDto> getAllBankAccounts() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<BankAccount> accounts = bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(companyId);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        return accounts.stream()
                .map(a -> toResponseDto(a, balances.getOrDefault(a.getAccountId(), BigDecimal.ZERO)))
                .toList();
    }

    public BankAccountResponseDto getBankAccountById(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankAccount account = findActiveAccountById(accountId, companyId);
        return toResponseDto(account, fetchBalance(accountId, companyId));
    }

    public BankAccountResponseDto updateBankAccount(Long accountId, BankAccountRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankAccount account = findActiveAccountById(accountId, companyId);

        if (!account.getIban().equals(dto.iban()) &&
                bankAccountRepository.existsByIbanAndAccountIdNotAndCompanyCompanyIdAndIsDeletedFalse(dto.iban(), accountId, companyId)) {
            throw new RuntimeException("Bu IBAN zaten kayıtlı: " + dto.iban());
        }

        account.setBankName(dto.bankName());
        account.setIban(dto.iban());
        account.setCurrency(dto.currency());

        BankAccount updated = bankAccountRepository.save(account);
        return toResponseDto(updated, fetchBalance(accountId, companyId));
    }

    public void softDeleteBankAccount(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankAccount account = findActiveAccountById(accountId, companyId);

        if (transactionRepository.existsByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(accountId, companyId)) {
            throw new RuntimeException("Bu hesabın aktif işlemleri var, silinemez.");
        }

        account.setDeleted(true);
        account.setDeletedAt(LocalDateTime.now());
        bankAccountRepository.save(account);
    }

    public BankAccountResponseDto restoreBankAccount(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankAccount account = bankAccountRepository.findByAccountIdAndCompanyCompanyId(accountId, companyId)
                .orElseThrow(() -> new RuntimeException("Bank account not found with id: " + accountId));

        account.setDeleted(false);
        account.setDeletedAt(null);
        BankAccount restored = bankAccountRepository.save(account);
        return toResponseDto(restored, fetchBalance(accountId, companyId));
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<BankAccount> expired = bankAccountRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (BankAccount account : expired) {
            bankAccountRepository.delete(account);
        }
        return expired.size();
    }


    // PRIVATE METOTLAR

    private BankAccount findActiveAccountById(Long accountId, Long companyId) {
        return bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(accountId, companyId)
                .orElseThrow(() -> new RuntimeException("Bank account not found with id: " + accountId));
    }

    private BigDecimal fetchBalance(Long accountId, Long companyId) {
        BigDecimal balance = transactionRepository.calculateBalanceForAccount(
                accountId, companyId, TransactionType.INCOME);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    private Map<Long, BigDecimal> buildBalanceMap(Long companyId) {
        return transactionRepository
                .calculateBalancesByCompany(companyId, TransactionType.INCOME)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    private BankAccountResponseDto toResponseDto(BankAccount a, BigDecimal balance) {
        return new BankAccountResponseDto(
                a.getAccountId(),
                a.getBankName(),
                a.getIban(),
                a.getCurrency().name(),
                balance,
                a.isDeleted(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
