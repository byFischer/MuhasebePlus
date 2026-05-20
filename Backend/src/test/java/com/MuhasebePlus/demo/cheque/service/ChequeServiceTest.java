package com.MuhasebePlus.demo.cheque.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.entity.*;
import com.MuhasebePlus.demo.cheque.repository.ChequeMovementRepository;
import com.MuhasebePlus.demo.cheque.repository.ChequeRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
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
}
