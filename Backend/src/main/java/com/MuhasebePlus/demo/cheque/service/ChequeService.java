package com.MuhasebePlus.demo.cheque.service;

import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeDetailsDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeMovementResponseDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequePortfolioSummaryDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeResponseDto;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeMovement;
import com.MuhasebePlus.demo.cheque.entity.ChequeMovementType;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.cheque.repository.ChequeMovementRepository;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChequeService implements HardDeletable {

    private final ChequeRepository chequeRepository;
    private final ChequeMovementRepository movementRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SystemLogService systemLogService;
    private final JournalEntryService journalEntryService;
    private final AccountingPeriodGuard periodGuard;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public ChequeResponseDto createCheque(ChequeRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(dto.issueDate());

        if (chequeRepository.existsByChequeNumberAndCompanyCompanyId(dto.chequeNumber(), companyId)) {
            throw new BusinessException("Bu çek numarası zaten kayıtlı: " + dto.chequeNumber());
        }

        Cheque cheque = Cheque.builder()
                .company(companyRepository.getReferenceById(companyId))
                .chequeNumber(dto.chequeNumber())
                .chequeType(dto.chequeType())
                .chequeKind(dto.chequeKind())
                .customerId(dto.customerId())
                .drawerBank(dto.drawerBank())
                .drawerBranch(dto.drawerBranch())
                .drawerAccountNumber(dto.drawerAccountNumber())
                .amount(dto.amount())
                .currency(dto.currency() != null ? dto.currency() : Currency.TRY)
                .issueDate(dto.issueDate())
                .dueDate(dto.dueDate())
                .status(ChequeStatus.IN_PORTFOLIO)
                .notes(dto.notes())
                .build();

        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.REGISTERED, "MANUAL", null, null, null, null);
        systemLogService.log(LogLevel.INFO, "Çek portföye eklendi: " + dto.chequeNumber());
        return toResponseDto(saved);
    }

    public ChequeResponseDto getChequeById(Long chequeId) {
        Cheque cheque = findActive(chequeId);
        return toResponseDto(cheque);
    }

    public List<ChequeResponseDto> getAllCheques() {
        Long companyId = companyContext.getCurrentCompanyId();
        return chequeRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(companyId)
                .stream().map(this::toResponseDto).toList();
    }

    public List<ChequeResponseDto> getChequesByStatus(String status) {
        Long companyId = companyContext.getCurrentCompanyId();
        ChequeStatus chequeStatus = ChequeStatus.valueOf(status.toUpperCase());
        return chequeRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(companyId, chequeStatus)
                .stream().map(this::toResponseDto).toList();
    }

    public List<ChequeResponseDto> getUpcomingCheques(int days) {
        Long companyId = companyContext.getCurrentCompanyId();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        return chequeRepository.findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                        companyId, ChequeStatus.IN_PORTFOLIO, from, to)
                .stream().map(this::toResponseDto).toList();
    }

    public void deleteCheque(Long chequeId) {
        Cheque cheque = findActive(chequeId);
        periodGuard.assertOpen(cheque.getIssueDate());
        cheque.setDeleted(true);
        cheque.setDeletedAt(LocalDateTime.now());
        chequeRepository.save(cheque);
        systemLogService.log(LogLevel.WARNING, "Çek silindi: " + cheque.getChequeNumber());
    }

    // ── DURUM GEÇİŞLERİ ───────────────────────────────────────────────────────

    public ChequeResponseDto deposit(Long chequeId, Long bankAccountId) {
        Cheque cheque = findActive(chequeId);
        periodGuard.assertOpen(LocalDate.now());
        assertStatus(cheque, ChequeStatus.IN_PORTFOLIO, "Sadece portföydeki çekler bankaya verilebilir");

        cheque.setStatus(ChequeStatus.DEPOSITED);
        cheque.setBankAccountId(bankAccountId);
        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.DEPOSITED, "MANUAL", null, bankAccountId, null, null);
        systemLogService.log(LogLevel.INFO, "Çek bankaya tahsile verildi: " + cheque.getChequeNumber());
        return toResponseDto(saved);
    }

    public ChequeResponseDto markAsCollected(Long chequeId, CollectChequeRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(LocalDate.now());
        Cheque cheque = findActive(chequeId);
        assertStatus(cheque, List.of(ChequeStatus.IN_PORTFOLIO, ChequeStatus.DEPOSITED),
                "Tahsil edilebilir durumda değil");

        BankAccount bankAccount = bankAccountRepository
                .findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(dto.bankAccountId(), companyId)
                .orElseThrow(() -> new BusinessException("Banka hesabı bulunamadı"));

        // Tahsilat → Transaction üret
        Transaction tx = new Transaction();
        tx.setCompany(companyRepository.getReferenceById(companyId));
        tx.setAccountId(dto.bankAccountId());
        tx.setTransactionType(cheque.getChequeType() == ChequeType.RECEIVABLE
                ? TransactionType.INCOME : TransactionType.EXPENSE);
        tx.setAmount(cheque.getAmount());
        tx.setTransactionDate(LocalDate.now());
        tx.setDescription((cheque.getChequeKind() == ChequeKind.CHEQUE ? "Çek tahsilatı: " : "Senet tahsilatı: ")
                + cheque.getChequeNumber());
        tx.setCategory(cheque.getChequeKind() == ChequeKind.CHEQUE ? "Çek Tahsilatı" : "Senet Tahsilatı");
        tx.setRecurring(false);
        tx.setDeleted(false);
        cheque.setStatus(ChequeStatus.COLLECTED);
        cheque.setBankAccountId(dto.bankAccountId());
        Transaction savedTx = transactionRepository.save(tx);
        cheque.setTransactionId(savedTx.getTransactionId());
        Cheque saved = chequeRepository.save(cheque);
        if (saved.getInvoicePaymentId() != null) {
            journalEntryService.createForChequeSettlement(saved, savedTx);
        } else {
            journalEntryService.createForTransaction(savedTx);
        }
        recordMovement(saved, ChequeMovementType.COLLECTED, "MANUAL", null,
                dto.bankAccountId(), savedTx.getTransactionId(), null);
        systemLogService.log(LogLevel.INFO, "Çek tahsil edildi: " + cheque.getChequeNumber()
                + " → " + bankAccount.getBankName());
        return toResponseDto(saved);
    }

    public ChequeResponseDto markAsBounced(Long chequeId, BounceReasonRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(LocalDate.now());
        Cheque cheque = findActive(chequeId);
        assertStatus(cheque, List.of(ChequeStatus.IN_PORTFOLIO, ChequeStatus.DEPOSITED),
                "Karşılıksız işlemi uygulanamaz");

        cheque.setStatus(ChequeStatus.BOUNCED);
        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.BOUNCED, "MANUAL", null, null, null, dto.reason());

        // Fatura ödemesi olarak alınmışsa → ödemeyi iptal et, fatura pending'e döner
        if (cheque.getInvoicePaymentId() != null) {
            invoicePaymentRepository.findById(cheque.getInvoicePaymentId()).ifPresent(payment -> {
                if (!payment.isDeleted()) {
                    periodGuard.assertOpen(payment.getPaymentDate());
                    payment.setDeleted(true);
                    payment.setDeletedAt(LocalDateTime.now());
                    invoicePaymentRepository.save(payment);
                    journalEntryService.reverseForPayment(companyId, payment.getPaymentId(), "Çek karşılıksız");
                    recalculateInvoiceStatus(payment.getInvoiceId(), companyId);
                }
            });
        }

        systemLogService.log(LogLevel.WARNING, "Çek karşılıksız: " + cheque.getChequeNumber()
                + " — " + dto.reason());
        return toResponseDto(saved);
    }

    public ChequeResponseDto endorse(Long chequeId, EndorseChequeRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(LocalDate.now());
        Cheque cheque = findActive(chequeId);
        assertStatus(cheque, ChequeStatus.IN_PORTFOLIO, "Sadece portföydeki çekler ciro edilebilir");

        customerRepository.findByCustomerIdAndCompanyCompanyId(dto.toCustomerId(), companyId)
                .orElseThrow(() -> new BusinessException("Ciro alıcısı müşteri bulunamadı"));

        cheque.setStatus(ChequeStatus.ENDORSED);
        cheque.setEndorsedToCustomerId(dto.toCustomerId());
        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.ENDORSED, "MANUAL", null, null, null,
                "Ciro alıcısı: " + dto.toCustomerId());
        systemLogService.log(LogLevel.INFO, "Çek ciro edildi: " + cheque.getChequeNumber());
        return toResponseDto(saved);
    }

    public ChequeResponseDto cancel(Long chequeId, String reason) {
        Cheque cheque = findActive(chequeId);
        periodGuard.assertOpen(LocalDate.now());
        assertStatus(cheque, ChequeStatus.IN_PORTFOLIO, "Sadece portföydeki çekler iptal edilebilir");

        cheque.setStatus(ChequeStatus.CANCELLED);
        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.CANCELLED, "MANUAL", null, null, null, reason);
        systemLogService.log(LogLevel.WARNING, "Çek iptal edildi: " + cheque.getChequeNumber());
        return toResponseDto(saved);
    }

    // ── FATURA ÖDEMESİ ENTEGRASYONU ──────────────────────────────────────────

    public void cancelByInvoicePayment(Long paymentId, Long companyId, String reason) {
        periodGuard.assertOpen(LocalDate.now());
        chequeRepository.findByInvoicePaymentIdAndCompanyCompanyIdAndIsDeletedFalse(paymentId, companyId)
                .ifPresent(cheque -> {
                    cheque.setStatus(ChequeStatus.CANCELLED);
                    cheque.setDeleted(true);
                    cheque.setDeletedAt(LocalDateTime.now());
                    Cheque saved = chequeRepository.save(cheque);
                    recordMovement(saved, ChequeMovementType.CANCELLED, "INVOICE_PAYMENT",
                            paymentId, null, null, reason);
                });
    }

    /**
     * InvoicePaymentService.createPayment tarafından çağrılır.
     * paymentMethod=check seçildiğinde Transaction üretilmez; çek portföye girer.
     */
    public Cheque registerFromPayment(InvoicePayment savedPayment, ChequeDetailsDto details,
                                      Long customerId, InvoiceType invoiceType) {
        Long companyId = savedPayment.getCompany().getCompanyId();
        periodGuard.assertOpen(savedPayment.getPaymentDate());

        if (chequeRepository.existsByChequeNumberAndCompanyCompanyId(details.chequeNumber(), companyId)) {
            throw new BusinessException("Bu çek numarası zaten kayıtlı: " + details.chequeNumber());
        }

        Cheque cheque = Cheque.builder()
                .company(companyRepository.getReferenceById(companyId))
                .chequeNumber(details.chequeNumber())
                .chequeType(invoiceType == InvoiceType.purchase ? ChequeType.PAYABLE : ChequeType.RECEIVABLE)
                .chequeKind(details.chequeKind())
                .customerId(customerId)
                .drawerBank(details.drawerBank())
                .drawerBranch(details.drawerBranch())
                .drawerAccountNumber(details.drawerAccountNumber())
                .amount(savedPayment.getAmount())
                .currency(Currency.TRY)
                .issueDate(details.issueDate())
                .dueDate(details.dueDate())
                .status(ChequeStatus.IN_PORTFOLIO)
                .invoicePaymentId(savedPayment.getPaymentId())
                .build();

        Cheque saved = chequeRepository.save(cheque);
        recordMovement(saved, ChequeMovementType.REGISTERED, "INVOICE_PAYMENT",
                savedPayment.getPaymentId(), null, null, null);
        systemLogService.log(LogLevel.INFO,
                "Fatura ödemesi çek olarak portföye eklendi: " + details.chequeNumber());
        return saved;
    }

    // ── PORTFÖY ÖZETİ ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ChequePortfolioSummaryDto getPortfolioSummary() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Cheque> all = chequeRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(companyId);

        LocalDate today = LocalDate.now();
        BigDecimal zero = BigDecimal.ZERO;

        long inPortfolioCount = 0; BigDecimal inPortfolioAmt = zero;
        long depositedCount   = 0; BigDecimal depositedAmt   = zero;
        long collectedCount   = 0; BigDecimal collectedAmt   = zero;
        long bouncedCount     = 0; BigDecimal bouncedAmt     = zero;
        long due0To30         = 0; BigDecimal due0To30Amt    = zero;
        long due31To60        = 0; BigDecimal due31To60Amt   = zero;
        long due60Plus        = 0; BigDecimal due60PlusAmt   = zero;

        for (Cheque c : all) {
            switch (c.getStatus()) {
                case IN_PORTFOLIO -> { inPortfolioCount++; inPortfolioAmt = inPortfolioAmt.add(c.getAmount());
                    long days = today.until(c.getDueDate()).getDays();
                    if (days <= 30)      { due0To30++;   due0To30Amt   = due0To30Amt.add(c.getAmount()); }
                    else if (days <= 60) { due31To60++;  due31To60Amt  = due31To60Amt.add(c.getAmount()); }
                    else                 { due60Plus++;  due60PlusAmt  = due60PlusAmt.add(c.getAmount()); }
                }
                case DEPOSITED  -> { depositedCount++;  depositedAmt  = depositedAmt.add(c.getAmount()); }
                case COLLECTED  -> { collectedCount++;  collectedAmt  = collectedAmt.add(c.getAmount()); }
                case BOUNCED    -> { bouncedCount++;    bouncedAmt    = bouncedAmt.add(c.getAmount()); }
                default -> { /* ENDORSED, CANCELLED — özete dahil değil */ }
            }
        }

        long totalCount = inPortfolioCount + depositedCount + collectedCount + bouncedCount;
        BigDecimal totalAmt = inPortfolioAmt.add(depositedAmt).add(collectedAmt).add(bouncedAmt);

        return new ChequePortfolioSummaryDto(totalCount, totalAmt,
                inPortfolioCount, inPortfolioAmt,
                depositedCount, depositedAmt,
                collectedCount, collectedAmt,
                bouncedCount, bouncedAmt,
                due0To30, due0To30Amt,
                due31To60, due31To60Amt,
                due60Plus, due60PlusAmt);
    }

    // ── HARD DELETE ────────────────────────────────────────────────────────────

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Cheque> expired = chequeRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Cheque c : expired) {
            movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(c.getChequeId())
                    .forEach(movementRepository::delete);
            chequeRepository.delete(c);
        }
        return expired.size();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private Cheque findActive(Long chequeId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(chequeId, companyId)
                .orElseThrow(() -> new BusinessException("Çek bulunamadı: " + chequeId));
    }

    private void assertStatus(Cheque cheque, ChequeStatus expected, String message) {
        if (cheque.getStatus() != expected) {
            throw new BusinessException(message + ". Mevcut durum: " + cheque.getStatus());
        }
    }

    private void assertStatus(Cheque cheque, List<ChequeStatus> allowed, String message) {
        if (!allowed.contains(cheque.getStatus())) {
            throw new BusinessException(message + ". Mevcut durum: " + cheque.getStatus());
        }
    }

    private void recordMovement(Cheque cheque, ChequeMovementType type, String sourceType,
                                Long sourceId, Long bankAccountId, Long transactionId, String reason) {
        ChequeMovement movement = ChequeMovement.builder()
                .company(cheque.getCompany())
                .chequeId(cheque.getChequeId())
                .movementType(type)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .movementDate(LocalDate.now())
                .bankAccountId(bankAccountId)
                .transactionId(transactionId)
                .reason(reason)
                .build();
        movement.setDeleted(false);
        movementRepository.save(movement);
    }

    private void recalculateInvoiceStatus(Long invoiceId, Long companyId) {
        invoiceRepository.findByInvoiceIdAndCompanyCompanyId(invoiceId, companyId)
                .filter(inv -> !inv.isDeleted() && !inv.isCancelled())
                .ifPresent(invoice -> {
                    BigDecimal paidTotal = invoicePaymentRepository.sumAmountByInvoiceId(invoiceId)
                            .orElse(BigDecimal.ZERO);
                    BigDecimal total = invoice.getTotalAmount();
                    PaymentStatus newStatus;
                    if (paidTotal.compareTo(BigDecimal.ZERO) == 0)   newStatus = PaymentStatus.pending;
                    else if (paidTotal.compareTo(total) >= 0)         newStatus = PaymentStatus.paid;
                    else                                               newStatus = PaymentStatus.partially_paid;
                    invoice.setPaymentStatus(newStatus);
                    invoiceRepository.save(invoice);
                });
    }

    private ChequeResponseDto toResponseDto(Cheque c) {
        Long companyId = c.getCompany().getCompanyId();
        String customerName = null;
        if (c.getCustomerId() != null) {
            customerName = customerRepository
                    .findByCustomerIdAndCompanyCompanyId(c.getCustomerId(), companyId)
                    .map(cu -> cu.getName())
                    .orElse(null);
        }
        List<ChequeMovementResponseDto> movements =
                movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(c.getChequeId())
                        .stream()
                        .map(m -> new ChequeMovementResponseDto(
                                m.getMovementId(), m.getMovementType(), m.getSourceType(),
                                m.getMovementDate(), m.getBankAccountId(), m.getTransactionId(), m.getReason()))
                        .toList();
        return new ChequeResponseDto(
                c.getChequeId(), c.getChequeNumber(), c.getChequeType(), c.getChequeKind(),
                c.getCustomerId(), customerName, c.getDrawerBank(), c.getDrawerBranch(),
                c.getDrawerAccountNumber(), c.getAmount(), c.getCurrency(),
                c.getIssueDate(), c.getDueDate(), c.getStatus(),
                c.getBankAccountId(), c.getTransactionId(), c.getInvoicePaymentId(),
                c.getEndorsedToCustomerId(), c.getNotes(), movements);
    }
}
