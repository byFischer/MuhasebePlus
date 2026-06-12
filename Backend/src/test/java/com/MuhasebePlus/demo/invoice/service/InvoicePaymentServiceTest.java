package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeDetailsDto;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.cheque.service.ChequeService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
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
import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePaymentServiceTest {

    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private AccountingPeriodGuard periodGuard;
    @Mock private ChequeService chequeService;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private InvoicePaymentService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long INVOICE_ID = 5L;
    private static final Long BANK_ACCOUNT_ID = 10L;
    private static final Long PAYMENT_ID = 77L;
    private static final Long TRANSACTION_ID = 500L;
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 5, 10);

    private Company company;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");

        bankAccount = new BankAccount();
        bankAccount.setAccountId(BANK_ACCOUNT_ID);
        bankAccount.setBankName("Ziraat Bankası");
    }

    // ── createPayment: validation / rejection paths ──────────────────────────

    @Test
    void createPayment_whenPeriodClosed_throwsBeforeAnyLookup() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        doThrow(new ClosedPeriodException(2026, 4)).when(periodGuard).assertOpen(PAYMENT_DATE);

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(ClosedPeriodException.class);

        verify(invoiceRepository, never()).findById(any());
        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenInvoiceNotFound_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fatura bulunamadı");

        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenInvoiceBelongsToOtherCompany_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        Company otherCompany = new Company();
        otherCompany.setCompanyId(99L);
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        invoice.setCompany(otherCompany);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("erişim yetkiniz yok");

        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenInvoiceSoftDeleted_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        invoice.setDeleted(true);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fatura bulunamadı");

        verify(invoicePaymentRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createPayment_whenInvoiceIsDraft_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.draft)));

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Taslak faturalara ödeme alınamaz");

        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenInvoiceAlreadyPaid_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.paid)));

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ödenmiş faturalara ödeme alınamaz");

        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenBankAccountNotFound_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.pending)));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("400.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Banka hesabı bulunamadı");

        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void createPayment_whenAmountExceedsRemaining_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.partially_paid)));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        // 600 zaten ödenmiş, kalan 400 — 500 talep ediliyor
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
                .thenReturn(Optional.of(new BigDecimal("600.00")));

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, paymentDto("500.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kalan bakiyeyi aşıyor")
                .hasMessageContaining("400.00");

        verify(transactionRepository, never()).save(any());
        verify(invoicePaymentRepository, never()).save(any());
        verify(journalEntryService, never()).createForPayment(any(), any());
    }

    // ── createPayment: status transitions ─────────────────────────────────────

    @Test
    void createPayment_whenPartialPayment_setsStatusPartiallyPaidAndReturnsDto() {
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        stubSuccessfulTransferPath(invoice, "0.00", "400.00");

        InvoicePaymentResponseDto result = service.createPayment(INVOICE_ID, paymentDto("400.00"));

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.partially_paid);
        verify(invoiceRepository).save(invoice);

        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(result.invoiceId()).isEqualTo(INVOICE_ID);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.bank_transfer);
        assertThat(result.bankAccountId()).isEqualTo(BANK_ACCOUNT_ID);
        assertThat(result.bankAccountName()).isEqualTo("Ziraat Bankası");
    }

    @Test
    void createPayment_whenPaymentCompletesTotal_setsStatusPaid() {
        Invoice invoice = invoiceStub(PaymentStatus.partially_paid);
        // 600 ödenmişti, 400 daha geliyor → toplam 1000 = totalAmount
        stubSuccessfulTransferPath(invoice, "600.00", "1000.00");

        service.createPayment(INVOICE_ID, paymentDto("400.00"));

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.paid);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void createPayment_whenInvoiceOverdue_acceptsPaymentButKeepsOverdueStatus() {
        Invoice invoice = invoiceStub(PaymentStatus.overdue);
        stubSuccessfulTransferPath(invoice, "0.00", "400.00");

        service.createPayment(INVOICE_ID, paymentDto("400.00"));

        // recalculateInvoiceStatus overdue durumunda erken döner; status değişmez
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.overdue);
        verify(invoiceRepository, never()).save(any());
        verify(invoicePaymentRepository).save(any(InvoicePayment.class));
    }

    @Test
    void createPayment_whenInvoiceCancelledButPending_currentlyStillAcceptsPayment() {
        // Davranış tespiti: createPayment cancelled bayrağını KONTROL ETMEZ;
        // iptal edilmiş ama pending statüsündeki faturaya ödeme kabul edilir.
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        invoice.setCancelled(true);
        stubSuccessfulTransferPath(invoice, "0.00", "400.00");

        InvoicePaymentResponseDto result = service.createPayment(INVOICE_ID, paymentDto("400.00"));

        assertThat(result).isNotNull();
        verify(invoicePaymentRepository).save(any(InvoicePayment.class));
    }

    // ── createPayment: transaction & journal entry per payment ───────────────

    @Test
    void createPayment_saleInvoice_createsIncomeTransactionAndJournalEntry() {
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        stubSuccessfulTransferPath(invoice, "0.00", "400.00");

        service.createPayment(INVOICE_ID, paymentDto("400.00"));

        verify(transactionRepository).save(argThat(t ->
                t.getTransactionType() == TransactionType.INCOME
                        && t.getAmount().compareTo(new BigDecimal("400.00")) == 0
                        && t.getInvoiceId().equals(INVOICE_ID)
                        && t.getAccountId().equals(BANK_ACCOUNT_ID)
                        && t.getTransactionDate().equals(PAYMENT_DATE)
                        && t.getDescription().equals("Fatura tahsilatı: FT-2026-0001")
                        && t.getCategory().equals("Fatura Tahsilatı")
        ));
        verify(journalEntryService).createForPayment(
                argThat(p -> p.getPaymentId().equals(PAYMENT_ID)
                        && p.getTransactionId().equals(TRANSACTION_ID)),
                argThat(t -> t.getTransactionId().equals(TRANSACTION_ID))
        );
        verify(journalEntryService, never()).createForChequePayment(any(), any());
    }

    @Test
    void createPayment_purchaseInvoice_createsExpenseTransaction() {
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        invoice.setInvoiceType(InvoiceType.purchase);
        stubSuccessfulTransferPath(invoice, "0.00", "400.00");

        service.createPayment(INVOICE_ID, paymentDto("400.00"));

        verify(transactionRepository).save(argThat(t ->
                t.getTransactionType() == TransactionType.EXPENSE
                        && t.getDescription().equals("Fatura ödemesi: FT-2026-0001")
                        && t.getCategory().equals("Fatura Ödemesi")
        ));
    }

    // ── createPayment: cheque path ────────────────────────────────────────────

    @Test
    void createPayment_chequeWithoutDetails_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.pending)));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)).thenReturn(Optional.of(BigDecimal.ZERO));

        InvoicePaymentRequestDto dto = new InvoicePaymentRequestDto(
                new BigDecimal("400.00"), PAYMENT_DATE, PaymentMethod.check, BANK_ACCOUNT_ID, null, null);

        assertThatThrownBy(() -> service.createPayment(INVOICE_ID, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chequeDetails alanı zorunludur");

        verify(invoicePaymentRepository, never()).save(any());
        verify(chequeService, never()).registerFromPayment(any(), any(), any(), any());
    }

    @Test
    void createPayment_chequeWithDetails_registersChequeWithoutTransaction() {
        Invoice invoice = invoiceStub(PaymentStatus.pending);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
                .thenReturn(Optional.of(BigDecimal.ZERO), Optional.of(new BigDecimal("400.00")));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(invoicePaymentRepository.save(any())).thenAnswer(inv -> {
            InvoicePayment p = inv.getArgument(0);
            p.setPaymentId(PAYMENT_ID);
            return p;
        });
        Cheque cheque = Cheque.builder()
                .chequeId(70L)
                .company(company)
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("400.00"))
                .build();
        when(chequeService.registerFromPayment(any(InvoicePayment.class), any(ChequeDetailsDto.class),
                eq(44L), eq(InvoiceType.sale))).thenReturn(cheque);

        InvoicePaymentResponseDto result = service.createPayment(INVOICE_ID, chequePaymentDto("400.00"));

        // Çek ödemesinde Transaction üretilmez, transactionId null kalır
        verify(transactionRepository, never()).save(any());
        verify(invoicePaymentRepository).save(argThat(p ->
                p.getTransactionId() == null && p.getPaymentMethod() == PaymentMethod.check));
        verify(journalEntryService).createForChequePayment(any(InvoicePayment.class), eq(cheque));
        verify(journalEntryService, never()).createForPayment(any(), any());
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.partially_paid);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.check);
    }

    // ── getPaymentsByInvoiceId ────────────────────────────────────────────────

    @Test
    void getPaymentsByInvoiceId_mapsBankNamesAndFallsBackToUnknown() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoiceStub(PaymentStatus.partially_paid)));

        InvoicePayment p1 = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        InvoicePayment p2 = paymentStub(PaymentMethod.cash, null);
        p2.setPaymentId(78L);
        p2.setBankAccountId(20L);
        when(invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(List.of(p1, p2));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyId(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyId(20L, COMPANY_ID))
                .thenReturn(Optional.empty());

        List<InvoicePaymentResponseDto> result = service.getPaymentsByInvoiceId(INVOICE_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).bankAccountName()).isEqualTo("Ziraat Bankası");
        assertThat(result.get(1).bankAccountName()).isEqualTo("Bilinmeyen");
    }

    // ── deletePayment ─────────────────────────────────────────────────────────

    @Test
    void deletePayment_whenNotFound_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePayment(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ödeme kaydı bulunamadı");

        verify(journalEntryService, never()).reverseForPayment(any(), any(), any());
    }

    @Test
    void deletePayment_whenAlreadyDeleted_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoicePayment payment = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        payment.setDeleted(true);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.deletePayment(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten silinmiş");

        verify(invoicePaymentRepository, never()).save(any());
        verify(journalEntryService, never()).reverseForPayment(any(), any(), any());
    }

    @Test
    void deletePayment_whenPeriodClosed_throwsBeforeSoftDelete() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoicePayment payment = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(payment));
        doThrow(new ClosedPeriodException(2026, 5)).when(periodGuard).assertOpen(PAYMENT_DATE);

        assertThatThrownBy(() -> service.deletePayment(PAYMENT_ID))
                .isInstanceOf(ClosedPeriodException.class);

        assertThat(payment.isDeleted()).isFalse();
        verify(invoicePaymentRepository, never()).save(any());
        verify(journalEntryService, never()).reverseForPayment(any(), any(), any());
    }

    @Test
    void deletePayment_whenValid_softDeletesReversesJournalAndRevertsStatusToPending() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoicePayment payment = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(payment));

        Transaction transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

        Invoice invoice = invoiceStub(PaymentStatus.partially_paid);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        // Silme sonrası kalan ödeme toplamı 0 → pending'e döner
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)).thenReturn(Optional.of(BigDecimal.ZERO));

        service.deletePayment(PAYMENT_ID);

        assertThat(payment.isDeleted()).isTrue();
        assertThat(payment.getDeletedAt()).isNotNull();
        verify(invoicePaymentRepository).save(payment);
        verify(journalEntryService).reverseForPayment(COMPANY_ID, PAYMENT_ID, "Ödeme silindi");
        assertThat(transaction.isDeleted()).isTrue();
        verify(transactionRepository).save(transaction);
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.pending);
        verify(invoiceRepository).save(invoice);
        verify(chequeService, never()).cancelByInvoicePayment(any(), any(), any());
    }

    @Test
    void deletePayment_whenInvoiceWasPaid_revertsStatusToPartiallyPaid() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoicePayment payment = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        Invoice invoice = invoiceStub(PaymentStatus.paid);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        // 1000'lik faturada 400'lük ödeme silindi, 600 kaldı → partially_paid
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
                .thenReturn(Optional.of(new BigDecimal("600.00")));

        service.deletePayment(PAYMENT_ID);

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.partially_paid);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void deletePayment_whenCheckPayment_cancelsChequeAndSkipsTransaction() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoicePayment payment = paymentStub(PaymentMethod.check, null);
        when(invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(payment));

        Invoice invoice = invoiceStub(PaymentStatus.partially_paid);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)).thenReturn(Optional.of(BigDecimal.ZERO));

        service.deletePayment(PAYMENT_ID);

        verify(chequeService).cancelByInvoicePayment(PAYMENT_ID, COMPANY_ID, "Odeme silindi");
        verify(transactionRepository, never()).findById(any());
        verify(journalEntryService).reverseForPayment(COMPANY_ID, PAYMENT_ID, "Ödeme silindi");
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.pending);
    }

    // ── hardDeleteExpired ─────────────────────────────────────────────────────

    @Test
    void hardDeleteExpired_deletesPaymentsAndLinkedTransactions() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 1, 1, 0, 0);
        InvoicePayment withTransaction = paymentStub(PaymentMethod.bank_transfer, TRANSACTION_ID);
        InvoicePayment withoutTransaction = paymentStub(PaymentMethod.check, null);
        withoutTransaction.setPaymentId(78L);
        when(invoicePaymentRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(withTransaction, withoutTransaction));

        Transaction transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

        int result = service.hardDeleteExpired(cutoff);

        assertThat(result).isEqualTo(2);
        verify(transactionRepository).delete(transaction);
        verify(invoicePaymentRepository).delete(withTransaction);
        verify(invoicePaymentRepository).delete(withoutTransaction);
        verify(transactionRepository, times(1)).findById(any());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private Invoice invoiceStub(PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("FT-2026-0001");
        invoice.setCustomerId(44L);
        invoice.setInvoiceType(InvoiceType.sale);
        invoice.setInvoiceDate(LocalDate.of(2026, 5, 1));
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaymentStatus(status);
        return invoice;
    }

    private InvoicePaymentRequestDto paymentDto(String amount) {
        return new InvoicePaymentRequestDto(
                new BigDecimal(amount),
                PAYMENT_DATE,
                PaymentMethod.bank_transfer,
                BANK_ACCOUNT_ID,
                "Test ödemesi",
                null
        );
    }

    private InvoicePaymentRequestDto chequePaymentDto(String amount) {
        ChequeDetailsDto details = new ChequeDetailsDto(
                "CHK-0001",
                ChequeKind.CHEQUE,
                "Ziraat",
                "Merkez",
                "TR00-1234",
                PAYMENT_DATE,
                PAYMENT_DATE.plusMonths(1)
        );
        return new InvoicePaymentRequestDto(
                new BigDecimal(amount),
                PAYMENT_DATE,
                PaymentMethod.check,
                BANK_ACCOUNT_ID,
                "Çek ödemesi",
                details
        );
    }

    private InvoicePayment paymentStub(PaymentMethod method, Long transactionId) {
        InvoicePayment payment = InvoicePayment.builder()
                .paymentId(PAYMENT_ID)
                .company(company)
                .invoiceId(INVOICE_ID)
                .amount(new BigDecimal("400.00"))
                .paymentDate(PAYMENT_DATE)
                .paymentMethod(method)
                .bankAccountId(BANK_ACCOUNT_ID)
                .transactionId(transactionId)
                .build();
        payment.setDeleted(false);
        return payment;
    }

    /**
     * Banka havalesi ile başarılı ödeme yolu için ortak stub'lar.
     * sumAmountByInvoiceId ilk çağrıda mevcut toplamı (bakiye kontrolü),
     * ikinci çağrıda ödeme sonrası toplamı (status recalc) döner.
     */
    private void stubSuccessfulTransferPath(Invoice invoice, String paidBefore, String paidAfter) {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(BANK_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
                .thenReturn(Optional.of(new BigDecimal(paidBefore)), Optional.of(new BigDecimal(paidAfter)));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setTransactionId(TRANSACTION_ID);
            return t;
        });
        when(invoicePaymentRepository.save(any())).thenAnswer(inv -> {
            InvoicePayment p = inv.getArgument(0);
            p.setPaymentId(PAYMENT_ID);
            return p;
        });
    }
}
