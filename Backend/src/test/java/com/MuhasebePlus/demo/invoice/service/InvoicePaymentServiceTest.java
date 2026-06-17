package com.MuhasebePlus.demo.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeDetailsDto;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.service.ChequeService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoicePaymentServiceTest {

    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CompanyContext companyContext;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private AccountingPeriodGuard periodGuard;

    @Mock
    private ChequeService chequeService;

    @Mock
    private JournalEntryService journalEntryService;

    @InjectMocks
    private InvoicePaymentService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long INVOICE_ID = 11L;
    private static final Long BANK_ACCOUNT_ID = 22L;

    private Company company;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        bankAccount = new BankAccount();
        bankAccount.setAccountId(BANK_ACCOUNT_ID);
        bankAccount.setCompany(company);
        bankAccount.setBankName("Test Bank");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(
            company
        );
        when(
            bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(
                BANK_ACCOUNT_ID,
                COMPANY_ID
            )
        ).thenReturn(Optional.of(bankAccount));
        when(invoicePaymentRepository.save(any())).thenAnswer(inv -> {
            InvoicePayment payment = inv.getArgument(0);
            if (payment.getPaymentId() == null) {
                payment.setPaymentId(55L);
            }
            return payment;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction transaction = inv.getArgument(0);
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(88L);
            }
            return transaction;
        });
    }

    // Rejects payments for draft invoices before bank or transaction work starts.
    @Test
    void createPayment_whenInvoiceDraft_throwsBusinessException() {
        Invoice invoice = invoice(
            PaymentStatus.draft,
            InvoiceType.sale,
            "250.00"
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );

        assertThatThrownBy(() ->
            service.createPayment(
                INVOICE_ID,
                request("50.00", PaymentMethod.cash)
            )
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Taslak");

        verify(
            bankAccountRepository,
            never()
        ).findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    // Rejects payments that exceed the remaining invoice balance.
    @Test
    void createPayment_whenAmountExceedsRemainingBalance_throwsBusinessException() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.sale,
            "250.00"
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(
            invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)
        ).thenReturn(Optional.of(new BigDecimal("200.00")));

        assertThatThrownBy(() ->
            service.createPayment(
                INVOICE_ID,
                request("60.00", PaymentMethod.cash)
            )
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Kalan");

        verify(transactionRepository, never()).save(any());
        verify(invoicePaymentRepository, never()).save(any());
    }

    // Creates income transaction and partially-paid status for a sale invoice payment.
    @Test
    void createPayment_whenSaleInvoicePayment_createsIncomeTransactionAndPartialStatus() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.sale,
            "250.00"
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
            .thenReturn(Optional.of(new BigDecimal("50.00")))
            .thenReturn(Optional.of(new BigDecimal("150.00")));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(
            Transaction.class
        );

        InvoicePaymentResponseDto result = service.createPayment(
            INVOICE_ID,
            request("100.00", PaymentMethod.bank_transfer)
        );

        assertThat(result.paymentId()).isEqualTo(55L);
        assertThat(result.bankAccountName()).isEqualTo("Test Bank");
        assertThat(invoice.getPaymentStatus()).isEqualTo(
            PaymentStatus.partially_paid
        );
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo(
            TransactionType.INCOME
        );
        assertThat(transactionCaptor.getValue().getInvoiceId()).isEqualTo(
            INVOICE_ID
        );
        verify(journalEntryService).createForPayment(
            any(InvoicePayment.class),
            any(Transaction.class)
        );
        verify(invoiceRepository).save(invoice);
    }

    // Creates expense transaction for purchase invoice payments.
    @Test
    void createPayment_whenPurchaseInvoicePayment_createsExpenseTransaction() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.purchase,
            "100.00"
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
            .thenReturn(Optional.of(BigDecimal.ZERO))
            .thenReturn(Optional.of(new BigDecimal("100.00")));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(
            Transaction.class
        );

        service.createPayment(
            INVOICE_ID,
            request("100.00", PaymentMethod.cash)
        );

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.paid);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo(
            TransactionType.EXPENSE
        );
        assertThat(transactionCaptor.getValue().getCategory()).contains(
            "Fatura"
        );
    }

    // Requires cheque details when payment method is check.
    @Test
    void createPayment_whenCheckWithoutDetails_throwsBusinessException() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.sale,
            "250.00"
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(
            invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)
        ).thenReturn(Optional.of(BigDecimal.ZERO));

        assertThatThrownBy(() ->
            service.createPayment(
                INVOICE_ID,
                request("50.00", PaymentMethod.check)
            )
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("chequeDetails");

        verify(transactionRepository, never()).save(any());
    }

    // Registers cheque payments without creating bank transaction rows.
    @Test
    void createPayment_whenCheckWithDetails_registersChequeAndSkipsTransaction() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.sale,
            "250.00"
        );
        Cheque cheque = new Cheque();
        cheque.setChequeId(44L);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID))
            .thenReturn(Optional.of(BigDecimal.ZERO))
            .thenReturn(Optional.of(new BigDecimal("50.00")));
        when(
            chequeService.registerFromPayment(
                any(InvoicePayment.class),
                any(ChequeDetailsDto.class),
                eq(99L),
                eq(InvoiceType.sale)
            )
        ).thenReturn(cheque);

        InvoicePaymentResponseDto result = service.createPayment(
            INVOICE_ID,
            checkRequest()
        );

        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.check);
        assertThat(result.bankAccountName()).isEqualTo("Test Bank");
        verify(transactionRepository, never()).save(any());
        verify(chequeService).registerFromPayment(
            any(InvoicePayment.class),
            any(ChequeDetailsDto.class),
            eq(99L),
            eq(InvoiceType.sale)
        );
        verify(journalEntryService).createForChequePayment(
            any(InvoicePayment.class),
            eq(cheque)
        );
    }

    // Lists invoice payments and resolves bank account names.
    @Test
    void getPaymentsByInvoiceId_whenPaymentsExist_mapsBankNames() {
        Invoice invoice = invoice(
            PaymentStatus.pending,
            InvoiceType.sale,
            "250.00"
        );
        InvoicePayment payment = payment(
            55L,
            INVOICE_ID,
            "30.00",
            PaymentMethod.cash
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(
            invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(
                INVOICE_ID,
                COMPANY_ID
            )
        ).thenReturn(List.of(payment));
        when(
            bankAccountRepository.findByAccountIdAndCompanyCompanyId(
                BANK_ACCOUNT_ID,
                COMPANY_ID
            )
        ).thenReturn(Optional.of(bankAccount));

        List<InvoicePaymentResponseDto> result = service.getPaymentsByInvoiceId(
            INVOICE_ID
        );

        assertThat(result)
            .singleElement()
            .satisfies(dto -> {
                assertThat(dto.paymentId()).isEqualTo(55L);
                assertThat(dto.bankAccountName()).isEqualTo("Test Bank");
            });
    }

    // Rejects deleting an already soft-deleted payment.
    @Test
    void deletePayment_whenAlreadyDeleted_throwsBusinessException() {
        InvoicePayment payment = payment(
            55L,
            INVOICE_ID,
            "30.00",
            PaymentMethod.cash
        );
        payment.setDeleted(true);
        when(
            invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(
                55L,
                COMPANY_ID
            )
        ).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.deletePayment(55L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("zaten");

        verify(journalEntryService, never()).reverseForPayment(
            any(),
            any(),
            any()
        );
    }

    // Soft deletes payment, reverses accounting entry, deletes transaction, and recalculates invoice status.
    @Test
    void deletePayment_whenCashPayment_marksPaymentAndTransactionDeleted() {
        Invoice invoice = invoice(
            PaymentStatus.partially_paid,
            InvoiceType.sale,
            "250.00"
        );
        InvoicePayment payment = payment(
            55L,
            INVOICE_ID,
            "30.00",
            PaymentMethod.cash
        );
        payment.setTransactionId(88L);
        Transaction transaction = new Transaction();
        transaction.setTransactionId(88L);
        when(
            invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(
                55L,
                COMPANY_ID
            )
        ).thenReturn(Optional.of(payment));
        when(transactionRepository.findById(88L)).thenReturn(
            Optional.of(transaction)
        );
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(
            invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)
        ).thenReturn(Optional.of(BigDecimal.ZERO));

        service.deletePayment(55L);

        assertThat(payment.isDeleted()).isTrue();
        assertThat(payment.getDeletedAt()).isNotNull();
        assertThat(transaction.isDeleted()).isTrue();
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.pending);
        verify(journalEntryService).reverseForPayment(
            eq(COMPANY_ID),
            eq(55L),
            anyString()
        );
        verify(transactionRepository).save(transaction);
        verify(invoiceRepository).save(invoice);
    }

    // Cancels linked cheque portfolio record when a cheque payment is deleted.
    @Test
    void deletePayment_whenCheckPayment_cancelsLinkedCheque() {
        Invoice invoice = invoice(
            PaymentStatus.partially_paid,
            InvoiceType.sale,
            "250.00"
        );
        InvoicePayment payment = payment(
            55L,
            INVOICE_ID,
            "30.00",
            PaymentMethod.check
        );
        when(
            invoicePaymentRepository.findByPaymentIdAndCompanyCompanyId(
                55L,
                COMPANY_ID
            )
        ).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(
            Optional.of(invoice)
        );
        when(
            invoicePaymentRepository.sumAmountByInvoiceId(INVOICE_ID)
        ).thenReturn(Optional.of(BigDecimal.ZERO));

        service.deletePayment(55L);

        verify(chequeService).cancelByInvoicePayment(
            55L,
            COMPANY_ID,
            "Odeme silindi"
        );
        verify(transactionRepository, never()).save(any());
    }

    // Permanently removes expired soft-deleted payments and their transaction rows.
    @Test
    void hardDeleteExpired_whenExpiredPaymentsExist_deletesPaymentsAndTransactions() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        InvoicePayment withTransaction = payment(
            55L,
            INVOICE_ID,
            "30.00",
            PaymentMethod.cash
        );
        withTransaction.setTransactionId(88L);
        InvoicePayment withoutTransaction = payment(
            56L,
            INVOICE_ID,
            "20.00",
            PaymentMethod.check
        );
        Transaction transaction = new Transaction();
        transaction.setTransactionId(88L);
        when(
            invoicePaymentRepository.findByIsDeletedTrueAndDeletedAtBefore(
                cutoff
            )
        ).thenReturn(List.of(withTransaction, withoutTransaction));
        when(transactionRepository.findById(88L)).thenReturn(
            Optional.of(transaction)
        );

        int result = service.hardDeleteExpired(cutoff);

        assertThat(result).isEqualTo(2);
        verify(transactionRepository).delete(transaction);
        verify(invoicePaymentRepository).delete(withTransaction);
        verify(invoicePaymentRepository).delete(withoutTransaction);
    }

    private InvoicePaymentRequestDto request(
        String amount,
        PaymentMethod paymentMethod
    ) {
        return new InvoicePaymentRequestDto(
            new BigDecimal(amount),
            LocalDate.of(2026, 5, 20),
            paymentMethod,
            BANK_ACCOUNT_ID,
            "Not",
            null
        );
    }

    private InvoicePaymentRequestDto checkRequest() {
        return new InvoicePaymentRequestDto(
            new BigDecimal("50.00"),
            LocalDate.of(2026, 5, 20),
            PaymentMethod.check,
            BANK_ACCOUNT_ID,
            "Cek notu",
            new ChequeDetailsDto(
                "CHK-1",
                ChequeKind.CHEQUE,
                "Drawer Bank",
                "Sube",
                "123",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1)
            )
        );
    }

    private Invoice invoice(
        PaymentStatus status,
        InvoiceType type,
        String totalAmount
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("INV-1");
        invoice.setCustomerId(99L);
        invoice.setInvoiceType(type);
        invoice.setPaymentStatus(status);
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setDeleted(false);
        return invoice;
    }

    private InvoicePayment payment(
        Long paymentId,
        Long invoiceId,
        String amount,
        PaymentMethod method
    ) {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(paymentId);
        payment.setCompany(company);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDate.of(2026, 5, 20));
        payment.setPaymentMethod(method);
        payment.setBankAccountId(BANK_ACCOUNT_ID);
        payment.setNotes("Not");
        payment.setDeleted(false);
        return payment;
    }
}
