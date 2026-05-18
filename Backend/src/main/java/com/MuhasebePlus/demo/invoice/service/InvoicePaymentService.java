package com.MuhasebePlus.demo.invoice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.dto.request.InvoicePaymentRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoicePaymentResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.cheque.service.ChequeService;
import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoicePaymentService implements HardDeletable {

    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final SystemLogService systemLogService;
    private final AccountingPeriodGuard periodGuard;
    private final ChequeService chequeService;
    private final JournalEntryService journalEntryService;

    public InvoicePaymentResponseDto createPayment(Long invoiceId, InvoicePaymentRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(dto.paymentDate());

        Invoice invoice = findActiveInvoice(invoiceId, companyId);

        if (invoice.getPaymentStatus() == PaymentStatus.draft) {
            throw new BusinessException("Taslak faturalara ödeme alınamaz");
        }
        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            throw new BusinessException("Ödenmiş faturalara ödeme alınamaz");
        }

        BankAccount bankAccount = bankAccountRepository
                .findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(dto.bankAccountId(), companyId)
                .orElseThrow(() -> new BusinessException("Banka hesabı bulunamadı veya şirketinize ait değil"));

        BigDecimal mevcutToplam = invoicePaymentRepository.sumAmountByInvoiceId(invoiceId).orElse(BigDecimal.ZERO);
        BigDecimal kalan = invoice.getTotalAmount().subtract(mevcutToplam);

        if (dto.amount().compareTo(kalan) > 0) {
            throw new BusinessException("Ödeme tutarı kalan bakiyeyi aşıyor. Kalan: " + kalan);
        }

        // Çek ile ödeme: Transaction üretilmez, çek portföye girer
        if (dto.paymentMethod() == PaymentMethod.check) {
            if (dto.chequeDetails() == null) {
                throw new BusinessException("Çek ile ödeme için chequeDetails alanı zorunludur");
            }

            InvoicePayment payment = InvoicePayment.builder()
                    .company(companyRepository.getReferenceById(companyId))
                    .invoiceId(invoiceId)
                    .amount(dto.amount())
                    .paymentDate(dto.paymentDate())
                    .paymentMethod(dto.paymentMethod())
                    .bankAccountId(dto.bankAccountId())
                    .transactionId(null)
                    .notes(dto.notes())
                    .build();
            payment.setDeleted(false);
            InvoicePayment savedPayment = invoicePaymentRepository.save(payment);

            chequeService.registerFromPayment(savedPayment, dto.chequeDetails(), invoice.getCustomerId());
            recalculateInvoiceStatus(invoiceId, companyId);
            systemLogService.log(LogLevel.INFO, "Çek ile fatura ödemesi portföye eklendi: "
                    + invoice.getInvoiceNumber() + " - " + dto.amount());
            return toResponseDto(savedPayment, bankAccount.getBankName());
        }

        Transaction transaction = new Transaction();
        transaction.setCompany(companyRepository.getReferenceById(companyId));
        transaction.setAccountId(dto.bankAccountId());
        transaction.setInvoiceId(invoiceId);
        transaction.setTransactionType(invoice.getInvoiceType() == InvoiceType.purchase ? TransactionType.EXPENSE : TransactionType.INCOME);
        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.paymentDate());
        transaction.setDescription((invoice.getInvoiceType() == InvoiceType.purchase ? "Fatura ödemesi: " : "Fatura tahsilatı: ") + invoice.getInvoiceNumber());
        transaction.setCategory(invoice.getInvoiceType() == InvoiceType.purchase ? "Fatura Ödemesi" : "Fatura Tahsilatı");
        transaction.setRecurring(false);
        transaction.setDeleted(false);

        Transaction savedTransaction = transactionRepository.save(transaction);

        InvoicePayment payment = InvoicePayment.builder()
                .company(companyRepository.getReferenceById(companyId))
                .invoiceId(invoiceId)
                .amount(dto.amount())
                .paymentDate(dto.paymentDate())
                .paymentMethod(dto.paymentMethod())
                .bankAccountId(dto.bankAccountId())
                .transactionId(savedTransaction.getTransactionId())
                .notes(dto.notes())
                .build();
        payment.setDeleted(false);

        InvoicePayment savedPayment = invoicePaymentRepository.save(payment);
        journalEntryService.createForPayment(savedPayment, savedTransaction);

        recalculateInvoiceStatus(invoiceId, companyId);

        systemLogService.log(LogLevel.INFO, "Fatura ödemesi kaydedildi: " + invoice.getInvoiceNumber() + " - " + dto.amount());

        return toResponseDto(savedPayment, bankAccount.getBankName());
    }

    public List<InvoicePaymentResponseDto> getPaymentsByInvoiceId(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();

        findActiveInvoice(invoiceId, companyId);

        List<InvoicePayment> payments = invoicePaymentRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);

        Map<Long, String> bankAccountNames = payments.stream()
                .map(InvoicePayment::getBankAccountId)
                .distinct()
                .map(accountId -> bankAccountRepository.findByAccountIdAndCompanyCompanyId(accountId, companyId))
                .filter(opt -> opt.isPresent())
                .collect(Collectors.toMap(
                        opt -> opt.get().getAccountId(),
                        opt -> opt.get().getBankName() != null ? opt.get().getBankName() : "Bilinmeyen"
                ));

        return payments.stream()
                .map(p -> toResponseDto(p, bankAccountNames.getOrDefault(p.getBankAccountId(), "Bilinmeyen")))
                .toList();
    }

    public void deletePayment(Long paymentId) {
        Long companyId = companyContext.getCurrentCompanyId();

        InvoicePayment payment = invoicePaymentRepository
                .findByPaymentIdAndCompanyCompanyId(paymentId, companyId)
                .orElseThrow(() -> new BusinessException("Ödeme kaydı bulunamadı"));

        periodGuard.assertOpen(payment.getPaymentDate());

        if (payment.isDeleted()) {
            throw new BusinessException("Ödeme kaydı zaten silinmiş");
        }

        payment.setDeleted(true);
        payment.setDeletedAt(LocalDateTime.now());
        invoicePaymentRepository.save(payment);
        journalEntryService.reverseForPayment(companyId, paymentId, "Ödeme silindi");

        if (payment.getTransactionId() != null) {
            transactionRepository.findById(payment.getTransactionId()).ifPresent(transaction -> {
                transaction.setDeleted(true);
                transaction.setDeletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
            });
        }

        recalculateInvoiceStatus(payment.getInvoiceId(), companyId);

        systemLogService.log(LogLevel.WARNING, "Fatura ödemesi silindi: " + paymentId);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<InvoicePayment> expired = invoicePaymentRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (InvoicePayment payment : expired) {
            if (payment.getTransactionId() != null) {
                transactionRepository.findById(payment.getTransactionId()).ifPresent(transactionRepository::delete);
            }
            invoicePaymentRepository.delete(payment);
        }
        return expired.size();
    }

    private void recalculateInvoiceStatus(Long invoiceId, Long companyId) {
        Invoice invoice = findActiveInvoice(invoiceId, companyId);

        PaymentStatus currentStatus = invoice.getPaymentStatus();
        if (currentStatus == PaymentStatus.draft || currentStatus == PaymentStatus.overdue) {
            return;
        }

        BigDecimal paidTotal = invoicePaymentRepository.sumAmountByInvoiceId(invoiceId).orElse(BigDecimal.ZERO);
        BigDecimal totalAmount = invoice.getTotalAmount();

        PaymentStatus newStatus;
        if (paidTotal.compareTo(BigDecimal.ZERO) == 0) {
            newStatus = PaymentStatus.pending;
        } else if (paidTotal.compareTo(totalAmount) >= 0) {
            newStatus = PaymentStatus.paid;
        } else {
            newStatus = PaymentStatus.partially_paid;
        }

        if (newStatus != currentStatus) {
            invoice.setPaymentStatus(newStatus);
            invoiceRepository.save(invoice);
        }
    }

    private Invoice findActiveInvoice(Long invoiceId, Long companyId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Fatura bulunamadı: " + invoiceId));

        if (!invoice.getCompany().getCompanyId().equals(companyId)) {
            throw new BusinessException("Bu kayda erişim yetkiniz yok");
        }
        if (invoice.isDeleted()) {
            throw new BusinessException("Fatura bulunamadı: " + invoiceId);
        }

        return invoice;
    }

    private InvoicePaymentResponseDto toResponseDto(InvoicePayment payment, String bankAccountName) {
        return new InvoicePaymentResponseDto(
                payment.getPaymentId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getBankAccountId(),
                bankAccountName,
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }
}
