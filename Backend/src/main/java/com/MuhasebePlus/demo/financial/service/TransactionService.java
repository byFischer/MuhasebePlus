package com.MuhasebePlus.demo.financial.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService implements HardDeletable {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final SystemLogService systemLogService;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final AccountingPeriodGuard periodGuard;
    private final JournalEntryService journalEntryService;


    // PUBLIC METOTLAR

    public TransactionResponseDto createTransaction(TransactionRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(dto.transactionDate());

        BankAccount account = validateAccount(dto.accountId(), companyId);
        validateInvoice(dto.invoiceId(), companyId);
        TransactionCategory transactionCategory = category(dto.transactionCategory());
        validateTransfer(dto.accountId(), dto.transferAccountId(), transactionCategory, companyId);
        TransactionType transactionType = transactionType(dto.transactionType(), transactionCategory);

        if (transactionType == TransactionType.EXPENSE) {
            assertSufficientBalance(account.getAccountId(), companyId, dto.amount());
        }

        Transaction tx = new Transaction();
        tx.setCompany(companyRepository.getReferenceById(companyId));
        tx.setAccountId(dto.accountId());
        tx.setTransferAccountId(transactionCategory == TransactionCategory.TRANSFER ? dto.transferAccountId() : null);
        tx.setInvoiceId(dto.invoiceId());
        tx.setTransactionType(transactionType);
        tx.setTransactionCategory(transactionCategory);
        tx.setAmount(dto.amount());
        tx.setTransactionDate(dto.transactionDate());
        tx.setDescription(dto.description());
        tx.setCategory(categoryLabel(dto.category(), transactionCategory));
        tx.setRecurring(Boolean.TRUE.equals(dto.isRecurring()));
        tx.setDeleted(false);

        Transaction saved = transactionRepository.save(tx);
        if (saved.getInvoiceId() == null) {
            journalEntryService.createForTransaction(saved);
        }
        systemLogService.log(LogLevel.INFO,
                "İşlem kaydedildi: " + saved.getTransactionType() + " " + saved.getAmount() + " TL (hesap=" + saved.getAccountId() + ")");
        log.info("Transaction created id={} type={} amount={} companyId={}", saved.getTransactionId(), saved.getTransactionType(), saved.getAmount(), companyId);
        return toResponseDto(saved);
    }

    public List<TransactionResponseDto> getAllTransactions() {
        Long companyId = companyContext.getCurrentCompanyId();
        return transactionRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getTransactionsByAccount(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        validateAccount(accountId, companyId);
        return transactionRepository
                .findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(accountId, companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getTransactionsByType(String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase(Locale.ROOT));
        return transactionRepository
                .findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(transactionType, companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Başlangıç tarihi bitiş tarihinden büyük olamaz");
        }
        return transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                        startDate, endDate, companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public TransactionResponseDto getTransactionById(Long transactionId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(findActiveTransactionById(transactionId, companyId));
    }

    public TransactionResponseDto updateTransaction(Long transactionId, TransactionRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Transaction tx = findActiveTransactionById(transactionId, companyId);
        periodGuard.assertOpen(tx.getTransactionDate());
        if (dto.transactionDate() != null) periodGuard.assertOpen(dto.transactionDate());

        BankAccount account = validateAccount(dto.accountId(), companyId);
        validateInvoice(dto.invoiceId(), companyId);
        TransactionCategory transactionCategory = category(dto.transactionCategory());
        validateTransfer(dto.accountId(), dto.transferAccountId(), transactionCategory, companyId);
        TransactionType transactionType = transactionType(dto.transactionType(), transactionCategory);

        // Bakiye kontrolu: yeni tutar - eski tutar (eski tutar bakiyeye geri eklenir)
        if (transactionType == TransactionType.EXPENSE) {
            BigDecimal currentBalance = fetchBalance(account.getAccountId(), companyId);
            BigDecimal effectiveBalance = restoreOldEffect(currentBalance, tx);
            if (effectiveBalance.compareTo(dto.amount()) < 0) {
                throw new RuntimeException("Bakiye yetersiz: hesap bakiyesi " + effectiveBalance + " TL");
            }
        }

        tx.setAccountId(dto.accountId());
        tx.setTransferAccountId(transactionCategory == TransactionCategory.TRANSFER ? dto.transferAccountId() : null);
        tx.setInvoiceId(dto.invoiceId());
        tx.setTransactionType(transactionType);
        tx.setTransactionCategory(transactionCategory);
        tx.setAmount(dto.amount());
        tx.setTransactionDate(dto.transactionDate());
        tx.setDescription(dto.description());
        tx.setCategory(categoryLabel(dto.category(), transactionCategory));
        tx.setRecurring(Boolean.TRUE.equals(dto.isRecurring()));

        Transaction updated = transactionRepository.save(tx);
        if (updated.getInvoiceId() == null) {
            Long companyId2 = updated.getCompany().getCompanyId();
            journalEntryService.reverseForTransaction(companyId2, transactionId, "Güncelleme");
            journalEntryService.createForTransaction(updated);
        }
        log.info("Transaction updated id={} companyId={}", transactionId, companyId);
        return toResponseDto(updated);
    }

    public void softDeleteTransaction(Long transactionId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Transaction tx = findActiveTransactionById(transactionId, companyId);
        periodGuard.assertOpen(tx.getTransactionDate());
        tx.setDeleted(true);
        tx.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(tx);
        if (tx.getInvoiceId() == null) {
            journalEntryService.reverseForTransaction(companyId, transactionId, "İşlem silindi");
        }
        log.warn("Transaction soft-deleted id={} companyId={}", transactionId, companyId);
    }

    public TransactionResponseDto restoreTransaction(Long transactionId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Transaction tx = transactionRepository.findByTransactionIdAndCompanyCompanyId(transactionId, companyId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));

        tx.setDeleted(false);
        tx.setDeletedAt(null);
        Transaction restored = transactionRepository.save(tx);
        return toResponseDto(restored);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Transaction> expired = transactionRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Transaction tx : expired) {
            transactionRepository.delete(tx);
        }
        return expired.size();
    }


    // PRIVATE METOTLAR

    private Transaction findActiveTransactionById(Long transactionId, Long companyId) {
        return transactionRepository.findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(transactionId, companyId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
    }

    private BankAccount validateAccount(Long accountId, Long companyId) {
        return bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(accountId, companyId)
                .orElseThrow(() -> new RuntimeException("Bank account not found with id: " + accountId));
    }

    private void validateInvoice(Long invoiceId, Long companyId) {
        if (invoiceId == null) return;
        invoiceRepository.findByInvoiceIdAndCompanyCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }

    private void assertSufficientBalance(Long accountId, Long companyId, BigDecimal amount) {
        BigDecimal balance = fetchBalance(accountId, companyId);
        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Bakiye yetersiz: hesap bakiyesi " + balance + " TL");
        }
    }

    private BigDecimal fetchBalance(Long accountId, Long companyId) {
        BigDecimal balance = transactionRepository.calculateBalanceForAccount(accountId, companyId, TransactionType.INCOME);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    /**
     * Update sirasinda mevcut transaction'in bakiyeye etkisini geri alir.
     * Boylece "yeni tutar - eski tutar" delta'si dogru hesaplanir.
     */
    private BigDecimal restoreOldEffect(BigDecimal currentBalance, Transaction oldTx) {
        if (oldTx.getTransactionType() == TransactionType.INCOME) {
            return currentBalance.subtract(oldTx.getAmount());
        } else {
            return currentBalance.add(oldTx.getAmount());
        }
    }

    private TransactionCategory category(TransactionCategory category) {
        return category != null ? category : TransactionCategory.GENERAL;
    }

    private TransactionType transactionType(TransactionType requested, TransactionCategory category) {
        return category == TransactionCategory.TRANSFER ? TransactionType.EXPENSE : requested;
    }

    private void validateTransfer(Long accountId, Long transferAccountId,
                                  TransactionCategory category, Long companyId) {
        if (category != TransactionCategory.TRANSFER) return;
        if (transferAccountId == null) {
            throw new RuntimeException("Virman icin hedef hesap zorunludur");
        }
        if (accountId.equals(transferAccountId)) {
            throw new RuntimeException("Virman kaynak ve hedef hesabi ayni olamaz");
        }
        validateAccount(transferAccountId, companyId);
    }

    private String categoryLabel(String category, TransactionCategory transactionCategory) {
        if (category != null && !category.isBlank()) return category;
        return switch (transactionCategory) {
            case BANK_FEE -> "Banka Masrafi";
            case POS_COLLECTION -> "POS Tahsilati";
            case CREDIT_CARD_PAYMENT -> "Kredi Karti Odemesi";
            case TRANSFER -> "Virman";
            case GENERAL -> null;
        };
    }

    private TransactionResponseDto toResponseDto(Transaction t) {
        return new TransactionResponseDto(
                t.getTransactionId(),
                t.getAccountId(),
                t.getTransferAccountId(),
                t.getInvoiceId(),
                t.getTransactionType() != null ? t.getTransactionType().name() : null,
                t.getTransactionCategory() != null ? t.getTransactionCategory().name() : TransactionCategory.GENERAL.name(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getDescription(),
                t.getCategory(),
                t.isRecurring(),
                t.isDeleted(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
