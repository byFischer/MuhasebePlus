package com.MuhasebePlus.demo.cheque.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeDetailsDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.entity.*;
import com.MuhasebePlus.demo.cheque.repository.ChequeMovementRepository;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChequeServiceTest {

    @Mock private ChequeRepository chequeRepository;
    @Mock private ChequeMovementRepository movementRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CustomerRepository customerRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private JournalEntryService journalEntryService;
    @Mock private AccountingPeriodGuard periodGuard;

    @InjectMocks
    private ChequeService chequeService;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(any()))
                .thenReturn(List.of());
    }

    @Test
    void getChequeById_whenFound_mapsCustomerAndMovements() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setCustomerId(20L);
        cheque.setDrawerBank("Test Bankasi");
        cheque.setDrawerBranch("Merkez");
        cheque.setDrawerAccountNumber("123456");
        cheque.setBankAccountId(10L);
        cheque.setTransactionId(100L);
        cheque.setInvoicePaymentId(200L);
        cheque.setEndorsedToCustomerId(30L);
        cheque.setNotes("Portfoy notu");
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        Customer customer = new Customer();
        customer.setName("ABC Musteri");
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(20L, COMPANY_ID))
                .thenReturn(Optional.of(customer));
        ChequeMovement movement = ChequeMovement.builder()
                .movementId(5L)
                .chequeId(1L)
                .movementType(ChequeMovementType.DEPOSITED)
                .sourceType("MANUAL")
                .movementDate(LocalDate.now())
                .bankAccountId(10L)
                .transactionId(100L)
                .reason("Bankaya verildi")
                .build();
        when(movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(1L))
                .thenReturn(List.of(movement));

        var response = chequeService.getChequeById(1L);

        assertThat(response.chequeId()).isEqualTo(1L);
        assertThat(response.customerName()).isEqualTo("ABC Musteri");
        assertThat(response.drawerBank()).isEqualTo("Test Bankasi");
        assertThat(response.bankAccountId()).isEqualTo(10L);
        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.invoicePaymentId()).isEqualTo(200L);
        assertThat(response.endorsedToCustomerId()).isEqualTo(30L);
        assertThat(response.notes()).isEqualTo("Portfoy notu");
        assertThat(response.movements()).singleElement()
                .satisfies(dto -> {
                    assertThat(dto.movementId()).isEqualTo(5L);
                    assertThat(dto.movementType()).isEqualTo(ChequeMovementType.DEPOSITED);
                    assertThat(dto.reason()).isEqualTo("Bankaya verildi");
                });
    }

    @Test
    void getChequeById_whenNotFound_throwsBusinessException() {
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(404L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chequeService.getChequeById(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("404");
    }

    @Test
    void listingMethods_delegateToCompanyScopedRepositoryQueries() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(COMPANY_ID))
                .thenReturn(List.of(cheque));
        when(chequeRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(COMPANY_ID, ChequeStatus.DEPOSITED))
                .thenReturn(List.of(buildCheque(2L, ChequeStatus.DEPOSITED)));
        when(chequeRepository.findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                eq(COMPANY_ID), eq(ChequeStatus.IN_PORTFOLIO), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(buildCheque(3L, ChequeStatus.IN_PORTFOLIO)));

        assertThat(chequeService.getAllCheques()).extracting("chequeNumber").containsExactly("CHK-001");
        assertThat(chequeService.getChequesByStatus("deposited")).singleElement()
                .extracting("status").isEqualTo(ChequeStatus.DEPOSITED);
        assertThat(chequeService.getUpcomingCheques(15)).singleElement()
                .extracting("chequeId").isEqualTo(3L);

        verify(chequeRepository).findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(COMPANY_ID);
        verify(chequeRepository).findByCompanyCompanyIdAndStatusAndIsDeletedFalse(COMPANY_ID, ChequeStatus.DEPOSITED);
        verify(chequeRepository).findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
                eq(COMPANY_ID), eq(ChequeStatus.IN_PORTFOLIO), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void deleteCheque_marksChequeSoftDeletedAndLogsWarning() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.deleteCheque(1L);

        assertThat(cheque.isDeleted()).isTrue();
        assertThat(cheque.getDeletedAt()).isNotNull();
        verify(periodGuard).assertOpen(cheque.getIssueDate());
        verify(chequeRepository).save(cheque);
    }

    // ── createCheque ──────────────────────────────────────────────────────────

    @Test
    void createCheque_whenDuplicateNumber_throwsBusinessException() {
        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-001", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> chequeService.createCheque(chequeRequestDto("CHK-001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten kayıtlı");

        verify(chequeRepository, never()).save(any());
    }

    @Test
    void createCheque_whenPeriodIsClosed_throwsBeforeSaving() {
        ChequeRequestDto dto = chequeRequestDto("CHK-CLOSED");
        doThrow(new ClosedPeriodException(dto.issueDate().getYear(), dto.issueDate().getMonthValue()))
                .when(periodGuard).assertOpen(dto.issueDate());

        assertThatThrownBy(() -> chequeService.createCheque(dto))
                .isInstanceOf(ClosedPeriodException.class);

        verify(chequeRepository, never()).save(any());
    }

    @Test
    void createCheque_whenValid_savesWithInPortfolioStatusAndRecordsMovement() {
        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-001", COMPANY_ID))
                .thenReturn(false);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(chequeRepository.save(any())).thenAnswer(inv -> {
            Cheque c = inv.getArgument(0);
            c.setChequeId(1L);
            return c;
        });

        chequeService.createCheque(chequeRequestDto("CHK-001"));

        verify(chequeRepository).save(argThat(c -> c.getStatus() == ChequeStatus.IN_PORTFOLIO));
        verify(movementRepository).save(argThat(m -> m.getMovementType() == ChequeMovementType.REGISTERED));
    }

    @Test
    void createCheque_whenCurrencyIsNull_defaultsToTry() {
        ChequeRequestDto dto = new ChequeRequestDto(
                "CHK-NULL-CUR",
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                null,
                "Test Bankasi",
                "Merkez",
                "123456789",
                new BigDecimal("5000.00"),
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null
        );
        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-NULL-CUR", COMPANY_ID))
                .thenReturn(false);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(chequeRepository.save(any())).thenAnswer(inv -> {
            Cheque c = inv.getArgument(0);
            c.setChequeId(10L);
            return c;
        });

        var response = chequeService.createCheque(dto);

        assertThat(response.currency()).isEqualTo(Currency.TRY);
        verify(chequeRepository).save(argThat(c -> c.getCurrency() == Currency.TRY));
    }

    // ── deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_whenStatusIsNotInPortfolio_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.DEPOSITED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        assertThatThrownBy(() -> chequeService.deposit(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("portföydeki çekler");
    }

    @Test
    void deposit_whenInPortfolio_changesStatusToDeposited() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.deposit(1L, 10L);

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.DEPOSITED);
        assertThat(cheque.getBankAccountId()).isEqualTo(10L);
        verify(movementRepository).save(argThat(m -> m.getMovementType() == ChequeMovementType.DEPOSITED));
    }

    // ── markAsCollected ───────────────────────────────────────────────────────

    @Test
    void markAsCollected_whenStatusIsEndorsed_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.ENDORSED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        assertThatThrownBy(() -> chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tahsil edilebilir");
    }

    @Test
    void markAsCollected_whenBankAccountMissing_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Banka hesab");

        verify(transactionRepository, never()).save(any());
        verify(chequeRepository, never()).save(any());
    }

    @Test
    void markAsCollected_whenInPortfolio_createsTransactionAndChangesStatusToCollected() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountId(10L);
        bankAccount.setBankName("Test Bankası");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(100L);
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L));

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.COLLECTED);
        assertThat(cheque.getTransactionId()).isEqualTo(100L);
        verify(journalEntryService).createForTransaction(savedTx);
        verify(movementRepository).save(argThat(m -> m.getMovementType() == ChequeMovementType.COLLECTED));
    }

    @Test
    void markAsCollected_whenInvoicePaymentCheque_usesChequeSettlementJournal() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(50L);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountId(10L);
        bankAccount.setBankName("Test Bankasi");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(101L);
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L));

        verify(journalEntryService).createForChequeSettlement(cheque, savedTx);
        verify(journalEntryService, never()).createForTransaction(savedTx);
    }

    @Test
    void markAsCollected_whenDeposited_alsoWorks() {
        Cheque cheque = buildCheque(1L, ChequeStatus.DEPOSITED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountId(10L);
        bankAccount.setBankName("Test Bankası");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(200L);
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L));

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.COLLECTED);
    }

    @Test
    void markAsCollected_whenPayablePromissoryNote_createsExpenseTransactionText() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setChequeType(ChequeType.PAYABLE);
        cheque.setChequeKind(ChequeKind.PROMISSORY_NOTE);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountId(10L);
        bankAccount.setBankName("Test Bankasi");
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(300L);
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.markAsCollected(1L, new CollectChequeRequestDto(10L));

        verify(transactionRepository).save(argThat(tx ->
                tx.getTransactionType() == com.MuhasebePlus.demo.financial.entity.TransactionType.EXPENSE
                        && tx.getDescription().contains("Senet")
                        && tx.getCategory().contains("Senet")
        ));
    }

    // ── markAsBounced ─────────────────────────────────────────────────────────

    @Test
    void markAsBounced_whenStatusIsCollected_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.COLLECTED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        assertThatThrownBy(() -> chequeService.markAsBounced(1L, new BounceReasonRequestDto("Hata")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void markAsBounced_whenNoInvoicePaymentId_onlyChangesStatusToBounced() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(null);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.markAsBounced(1L, new BounceReasonRequestDto("Hesapta para yok"));

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.BOUNCED);
        verify(invoicePaymentRepository, never()).findById(any());
        verify(journalEntryService, never()).reverseForPayment(any(), any(), any());
    }

    @Test
    void markAsBounced_whenHasInvoicePaymentId_cancelsPaymentAndCallsJournalReversal() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(50L);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(50L);
        payment.setInvoiceId(20L);
        payment.setDeleted(false);
        when(invoicePaymentRepository.findById(50L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(20L, COMPANY_ID))
                .thenReturn(Optional.empty());

        chequeService.markAsBounced(1L, new BounceReasonRequestDto("Karşılıksız"));

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.BOUNCED);
        assertThat(payment.isDeleted()).isTrue();
        assertThat(payment.getDeletedAt()).isNotNull();
        verify(journalEntryService).reverseForPayment(COMPANY_ID, 50L, "Çek karşılıksız");
    }

    @Test
    void markAsBounced_whenInvoiceHasNoRemainingPayments_setsInvoicePending() {
        Invoice invoice = invoice(20L, new BigDecimal("1000.00"));

        bounceInvoicePaymentAndReturnInvoiceStatus(invoice, BigDecimal.ZERO);

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.pending);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markAsBounced_whenInvoiceStillFullyPaid_setsInvoicePaid() {
        Invoice invoice = invoice(20L, new BigDecimal("1000.00"));

        bounceInvoicePaymentAndReturnInvoiceStatus(invoice, new BigDecimal("1000.00"));

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.paid);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markAsBounced_whenInvoiceStillPartlyPaid_setsInvoicePartiallyPaid() {
        Invoice invoice = invoice(20L, new BigDecimal("1000.00"));

        bounceInvoicePaymentAndReturnInvoiceStatus(invoice, new BigDecimal("400.00"));

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.partially_paid);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markAsBounced_whenPaymentAlreadyDeleted_doesNotReverseAgain() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(50L);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(50L);
        payment.setDeleted(true); // zaten silinmiş
        when(invoicePaymentRepository.findById(50L)).thenReturn(Optional.of(payment));

        chequeService.markAsBounced(1L, new BounceReasonRequestDto("Karşılıksız"));

        verify(journalEntryService, never()).reverseForPayment(any(), any(), any());
    }

    // ── endorse ───────────────────────────────────────────────────────────────

    @Test
    void endorse_whenStatusIsNotInPortfolio_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.COLLECTED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        assertThatThrownBy(() -> chequeService.endorse(1L, new EndorseChequeRequestDto(99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("portföydeki çekler");
    }

    @Test
    void endorse_whenCustomerNotFound_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chequeService.endorse(1L, new EndorseChequeRequestDto(99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("müşteri bulunamadı");
    }

    @Test
    void endorse_whenInPortfolio_changesStatusToEndorsed() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(99L, COMPANY_ID))
                .thenReturn(Optional.of(new Customer()));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.endorse(1L, new EndorseChequeRequestDto(99L));

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.ENDORSED);
        assertThat(cheque.getEndorsedToCustomerId()).isEqualTo(99L);
        verify(movementRepository).save(argThat(m -> m.getMovementType() == ChequeMovementType.ENDORSED));
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_whenStatusIsNotInPortfolio_throwsBusinessException() {
        Cheque cheque = buildCheque(1L, ChequeStatus.ENDORSED);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));

        assertThatThrownBy(() -> chequeService.cancel(1L, "İptal"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("portföydeki çekler");
    }

    @Test
    void cancel_whenInPortfolio_changesStatusToCancelled() {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.cancel(1L, "Test iptali");

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.CANCELLED);
        verify(movementRepository).save(argThat(m -> m.getMovementType() == ChequeMovementType.CANCELLED));
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    @Test
    void registerFromPayment_whenPurchaseInvoice_createsPayableCheque() {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(60L);
        payment.setCompany(company);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(new BigDecimal("5000.00"));
        ChequeDetailsDto details = new ChequeDetailsDto(
                "CHK-PAY-001",
                ChequeKind.CHEQUE,
                "Test Bankasi",
                "Merkez",
                "123456789",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-PAY-001", COMPANY_ID))
                .thenReturn(false);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(chequeRepository.save(any())).thenAnswer(inv -> {
            Cheque c = inv.getArgument(0);
            c.setChequeId(60L);
            return c;
        });

        Cheque saved = chequeService.registerFromPayment(payment, details, 99L, InvoiceType.purchase);

        assertThat(saved.getChequeType()).isEqualTo(ChequeType.PAYABLE);
        assertThat(saved.getInvoicePaymentId()).isEqualTo(60L);
        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == ChequeMovementType.REGISTERED
                        && m.getSourceId().equals(60L)
                        && "INVOICE_PAYMENT".equals(m.getSourceType())
        ));
    }

    @Test
    void registerFromPayment_whenSalesInvoice_createsReceivableCheque() {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(61L);
        payment.setCompany(company);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(new BigDecimal("7500.00"));
        ChequeDetailsDto details = chequeDetailsDto("CHK-REC-001");

        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-REC-001", COMPANY_ID))
                .thenReturn(false);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cheque saved = chequeService.registerFromPayment(payment, details, 99L, InvoiceType.sale);

        assertThat(saved.getChequeType()).isEqualTo(ChequeType.RECEIVABLE);
        assertThat(saved.getCustomerId()).isEqualTo(99L);
        assertThat(saved.getAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void registerFromPayment_whenDuplicateChequeNumber_throwsBusinessException() {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(62L);
        payment.setCompany(company);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(new BigDecimal("1000.00"));
        ChequeDetailsDto details = chequeDetailsDto("CHK-DUP");
        when(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-DUP", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> chequeService.registerFromPayment(payment, details, 99L, InvoiceType.sale))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CHK-DUP");

        verify(chequeRepository, never()).save(any());
    }

    @Test
    void cancelByInvoicePayment_marksLinkedChequeDeleted() {
        Cheque cheque = buildCheque(2L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(60L);
        when(chequeRepository.findByInvoicePaymentIdAndCompanyCompanyIdAndIsDeletedFalse(60L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chequeService.cancelByInvoicePayment(60L, COMPANY_ID, "Odeme silindi");

        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.CANCELLED);
        assertThat(cheque.isDeleted()).isTrue();
        assertThat(cheque.getDeletedAt()).isNotNull();
        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == ChequeMovementType.CANCELLED
                        && m.getSourceId().equals(60L)
                        && "INVOICE_PAYMENT".equals(m.getSourceType())
        ));
    }

    @Test
    void cancelByInvoicePayment_whenNoLinkedCheque_doesNothing() {
        when(chequeRepository.findByInvoicePaymentIdAndCompanyCompanyIdAndIsDeletedFalse(60L, COMPANY_ID))
                .thenReturn(Optional.empty());

        chequeService.cancelByInvoicePayment(60L, COMPANY_ID, "Odeme silindi");

        verify(chequeRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void getPortfolioSummary_groupsStatusesAndDueBuckets() {
        LocalDate today = LocalDate.now();
        when(chequeRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(COMPANY_ID))
                .thenReturn(List.of(
                        summaryCheque(1L, ChequeStatus.IN_PORTFOLIO, "100.00", today.plusDays(10)),
                        summaryCheque(2L, ChequeStatus.IN_PORTFOLIO, "200.00", today.plusDays(45)),
                        summaryCheque(3L, ChequeStatus.IN_PORTFOLIO, "300.00", today.plusDays(90)),
                        summaryCheque(4L, ChequeStatus.DEPOSITED, "400.00", today.plusDays(5)),
                        summaryCheque(5L, ChequeStatus.COLLECTED, "500.00", today.plusDays(5)),
                        summaryCheque(6L, ChequeStatus.BOUNCED, "600.00", today.plusDays(5)),
                        summaryCheque(7L, ChequeStatus.ENDORSED, "700.00", today.plusDays(5)),
                        summaryCheque(8L, ChequeStatus.CANCELLED, "800.00", today.plusDays(5))
                ));

        var summary = chequeService.getPortfolioSummary();

        assertThat(summary.totalCount()).isEqualTo(6);
        assertThat(summary.totalAmount()).isEqualByComparingTo("2100.00");
        assertThat(summary.inPortfolioCount()).isEqualTo(3);
        assertThat(summary.inPortfolioAmount()).isEqualByComparingTo("600.00");
        assertThat(summary.depositedCount()).isEqualTo(1);
        assertThat(summary.collectedAmount()).isEqualByComparingTo("500.00");
        assertThat(summary.bouncedAmount()).isEqualByComparingTo("600.00");
        assertThat(summary.due0To30Count()).isEqualTo(1);
        assertThat(summary.due31To60Amount()).isEqualByComparingTo("200.00");
        assertThat(summary.due60PlusAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void hardDeleteExpired_deletesMovementsAndCheques() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        Cheque first = buildCheque(1L, ChequeStatus.CANCELLED);
        Cheque second = buildCheque(2L, ChequeStatus.CANCELLED);
        ChequeMovement movement = ChequeMovement.builder()
                .movementId(10L)
                .chequeId(1L)
                .movementType(ChequeMovementType.CANCELLED)
                .movementDate(LocalDate.now())
                .build();
        when(chequeRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));
        when(movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(1L))
                .thenReturn(List.of(movement));
        when(movementRepository.findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(2L))
                .thenReturn(List.of());

        int deleted = chequeService.hardDeleteExpired(cutoff);

        assertThat(deleted).isEqualTo(2);
        verify(movementRepository).delete(movement);
        verify(chequeRepository).delete(first);
        verify(chequeRepository).delete(second);
    }

    private void bounceInvoicePaymentAndReturnInvoiceStatus(Invoice invoice, BigDecimal paidTotalAfterBounce) {
        Cheque cheque = buildCheque(1L, ChequeStatus.IN_PORTFOLIO);
        cheque.setInvoicePaymentId(50L);
        when(chequeRepository.findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(50L);
        payment.setInvoiceId(20L);
        payment.setPaymentDate(LocalDate.now());
        payment.setDeleted(false);
        when(invoicePaymentRepository.findById(50L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(20L, COMPANY_ID))
                .thenReturn(Optional.of(invoice));
        when(invoicePaymentRepository.sumAmountByInvoiceId(20L))
                .thenReturn(Optional.of(paidTotalAfterBounce));

        chequeService.markAsBounced(1L, new BounceReasonRequestDto("Karsiliksiz"));
    }

    private Cheque buildCheque(Long chequeId, ChequeStatus status) {
        return Cheque.builder()
                .chequeId(chequeId)
                .company(company)
                .chequeNumber("CHK-00" + chequeId)
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("5000.00"))
                .currency(Currency.TRY)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(status)
                .build();
    }

    private Cheque summaryCheque(Long chequeId, ChequeStatus status, String amount, LocalDate dueDate) {
        Cheque cheque = buildCheque(chequeId, status);
        cheque.setAmount(new BigDecimal(amount));
        cheque.setDueDate(dueDate);
        return cheque;
    }

    private ChequeDetailsDto chequeDetailsDto(String chequeNumber) {
        return new ChequeDetailsDto(
                chequeNumber,
                ChequeKind.CHEQUE,
                "Test Bankasi",
                "Merkez",
                "123456789",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
    }

    private ChequeRequestDto chequeRequestDto(String chequeNumber) {
        return new ChequeRequestDto(
                chequeNumber,
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                null,
                "Test Bankası",
                "Merkez",
                "123456789",
                new BigDecimal("5000.00"),
                Currency.TRY,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null
        );
    }

    private Invoice invoice(Long invoiceId, BigDecimal totalAmount) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompany(company);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setTotalAmount(totalAmount);
        invoice.setPaymentStatus(PaymentStatus.pending);
        invoice.setDeleted(false);
        invoice.setCancelled(false);
        return invoice;
    }
}
