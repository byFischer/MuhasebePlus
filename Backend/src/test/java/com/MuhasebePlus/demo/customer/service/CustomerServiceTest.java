package com.MuhasebePlus.demo.customer.service;

import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.ChartOfAccountService;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerActivityDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerAgingDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerNote;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerNoteRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
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
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerNoteRepository customerNoteRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private ChartOfAccountService chartOfAccountService;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private CustomerService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
    }

    // Tests duplicate tax number protection before creating a customer.
    @Test
    void createCustomer_whenTaxNumberExists_throwsAndDoesNotSave() {
        CustomerRequestDto dto = request("ABC Ltd", "1234567890", CustomerRole.BUYER, BigDecimal.ZERO, null);
        when(customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse("1234567890", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tax number");

        verify(customerRepository, never()).save(any());
        verify(journalEntryService, never()).createForCustomerOpening(any());
    }

    // Tests buyer creation generates a 120 customer account and opening journal.
    @Test
    void createCustomer_whenBuyerHasNoAccountCode_generatesReceivableAccountAndOpeningJournal() {
        CustomerRequestDto dto = request("ABC Ltd", "1234567890", CustomerRole.BUYER, new BigDecimal("1500.00"), null);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(chartOfAccountService.getOrCreateLeafAccount(COMPANY_ID, "120", "ABC Ltd", AccountType.ASSET))
                .thenReturn("120.001");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            if (customer.getCustomerId() == null) {
                customer.setCustomerId(10L);
            }
            return customer;
        });

        CustomerResponseDto result = service.createCustomer(dto);

        assertThat(result.customerId()).isEqualTo(10L);
        assertThat(result.accountCode()).isEqualTo("120.001");
        assertThat(result.openingBalance()).isEqualByComparingTo("1500.00");
        verify(journalEntryService).createForCustomerOpening(any(Customer.class));
    }

    // Tests seller or negative opening balance uses the vendor 320 parent account.
    @Test
    void createCustomer_whenSellerHasNoAccountCode_generatesVendorAccount() {
        CustomerRequestDto dto = request("XYZ Tedarik", "1234567891", CustomerRole.SELLER, new BigDecimal("-900.00"), null);
        when(chartOfAccountService.isAccountingSetup(COMPANY_ID)).thenReturn(true);
        when(chartOfAccountService.getOrCreateLeafAccount(COMPANY_ID, "320", "XYZ Tedarik", AccountType.LIABILITY))
                .thenReturn("320.001");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            customer.setCustomerId(11L);
            return customer;
        });

        CustomerResponseDto result = service.createCustomer(dto);

        assertThat(result.accountCode()).isEqualTo("320.001");
        assertThat(result.customerRole()).isEqualTo("SELLER");
    }

    // Tests balance calculation combines opening balance, sales, purchases, and payments.
    @Test
    void getAllCustomers_calculatesCurrentBalanceFromInvoicesAndPayments() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setOpeningBalance(new BigDecimal("100.00"));
        Invoice sale = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("1000.00"), PaymentStatus.pending);
        Invoice purchase = invoice(2L, 10L, InvoiceType.purchase, new BigDecimal("250.00"), PaymentStatus.pending);
        when(customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID)).thenReturn(List.of(customer));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID)).thenReturn(List.of(sale, purchase));
        when(invoicePaymentRepository.sumAmountByInvoiceId(1L)).thenReturn(Optional.of(new BigDecimal("300.00")));
        when(invoicePaymentRepository.sumAmountByInvoiceId(2L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(invoiceRepository.findCustomerIdsWithOverdueInvoices(COMPANY_ID)).thenReturn(List.of(10L));

        List<CustomerResponseDto> result = service.getAllCustomers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currentBalance()).isEqualByComparingTo("550.00");
        assertThat(result.get(0).hasOverdueInvoices()).isTrue();
    }

    // Tests customer update reverses and recreates opening journal when opening data changes.
    @Test
    void updateCustomer_whenOpeningBalanceChanges_reversesAndRecreatesOpeningJournal() {
        Customer existing = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        existing.setOpeningBalance(new BigDecimal("100.00"));
        existing.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        existing.setAccountCode("120.001");
        CustomerRequestDto dto = request("ABC Ltd", "1234567890", CustomerRole.BUYER,
                new BigDecimal("200.00"), "120.001");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of());

        CustomerResponseDto result = service.updateCustomer(10L, dto);

        assertThat(result.openingBalance()).isEqualByComparingTo("200.00");
        verify(journalEntryService).reverseForCustomerOpening(eq(COMPANY_ID), eq(10L), any());
        verify(journalEntryService).createForCustomerOpening(existing);
    }

    // Tests customers with active invoices cannot be soft deleted.
    @Test
    void softDeleteCustomer_whenActiveInvoicesExist_throwsAndDoesNotDelete() {
        when(invoiceRepository.existsByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.softDeleteCustomer(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("silinemez");

        verify(customerRepository, never()).findById(anyLong());
        verify(customerRepository, never()).save(any());
    }

    // Tests soft delete marks customer and reverses opening journal when no active invoices exist.
    @Test
    void softDeleteCustomer_whenNoActiveInvoices_marksDeletedAndReversesOpening() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        when(invoiceRepository.existsByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(false);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDeleteCustomer(10L);

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.getDeletedAt()).isNotNull();
        verify(journalEntryService).reverseForCustomerOpening(eq(COMPANY_ID), eq(10L), any());
    }

    // Tests restore clears delete metadata and recreates opening journal.
    @Test
    void restoreCustomer_whenDeleted_clearsDeletedFlagsAndCreatesOpeningJournal() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        customer.setAccountCode("120.001");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of());

        CustomerResponseDto result = service.restoreCustomer(10L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(customer.getDeletedAt()).isNull();
        verify(journalEntryService).createForCustomerOpening(customer);
    }

    // Tests activity statement orders opening, invoice, and payment with running balance.
    @Test
    void getCustomerActivity_whenSaleAndPaymentExist_returnsRunningBalanceLines() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setOpeningBalance(new BigDecimal("100.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        Invoice sale = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("500.00"), PaymentStatus.pending);
        sale.setInvoiceDate(LocalDate.of(2026, 1, 10));
        InvoicePayment payment = payment(90L, 1L, new BigDecimal("200.00"), LocalDate.of(2026, 1, 12));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of(sale));
        when(invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(payment));

        List<CustomerActivityDto> result = service.getCustomerActivity(
                10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CustomerActivityDto::type)
                .containsExactly("ACILIS", "FATURA", "TAHSILAT");
        assertThat(result.get(2).balance()).isEqualByComparingTo("400.00");
    }

    // Tests aging puts overdue receivables into their expected buckets net of payments.
    @Test
    void getCustomerAging_whenOpenSaleInvoicesAreOverdue_bucketsRemainingAmounts() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setAccountCode("120.001");
        Invoice invoice = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("1000.00"), PaymentStatus.pending);
        invoice.setDueDate(LocalDate.now().minusDays(40));
        when(invoiceRepository.findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of(invoice));
        when(customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(customer));
        when(invoicePaymentRepository.sumAmountByInvoiceId(1L)).thenReturn(Optional.of(new BigDecimal("250.00")));

        List<CustomerAgingDto> result = service.getCustomerAging();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalAR()).isEqualByComparingTo("750.00");
        assertThat(result.get(0).bucket31to60()).isEqualByComparingTo("750.00");
    }

    // Tests adding a customer note validates customer ownership and returns saved content.
    @Test
    void addNote_whenCustomerExists_savesAndReturnsNote() {
        when(customerRepository.existsByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(true);
        when(customerNoteRepository.save(any())).thenAnswer(inv -> {
            CustomerNote note = inv.getArgument(0);
            note.setNoteId(55L);
            return note;
        });

        CustomerNoteResponseDto result = service.addNote(10L, new CustomerNoteRequestDto("Aranacak"));

        assertThat(result.noteId()).isEqualTo(55L);
        assertThat(result.content()).isEqualTo("Aranacak");
    }

    // Tests note update and delete use company scoped note lookup.
    @Test
    void updateAndDeleteNote_whenNoteBelongsToCompany_updatesThenDeletes() {
        CustomerNote note = note(55L, 10L, "Eski not");
        when(customerNoteRepository.findById(55L)).thenReturn(Optional.of(note));
        when(customerNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerNoteResponseDto updated = service.updateNote(55L, new CustomerNoteRequestDto("Yeni not"));
        service.deleteNote(55L);

        assertThat(updated.content()).isEqualTo("Yeni not");
        verify(customerNoteRepository).delete(note);
    }

    // Tests expired soft-deleted customers delete dependent notes and return count.
    @Test
    void hardDeleteExpired_whenExpiredCustomersExist_deletesNotesAndCustomers() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        Customer first = customer(1L, "A", CustomerRole.BUYER);
        Customer second = customer(2L, "B", CustomerRole.SELLER);
        when(customerRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(customerNoteRepository).deleteByCustomerId(1L);
        verify(customerNoteRepository).deleteByCustomerId(2L);
        verify(customerRepository).delete(first);
        verify(customerRepository).delete(second);
    }

    private CustomerRequestDto request(String name, String taxNumber, CustomerRole role,
                                       BigDecimal openingBalance, String accountCode) {
        return new CustomerRequestDto(
                name,
                taxNumber,
                "Adres",
                "Istanbul",
                "5321234567",
                CustomerType.CORPORATE,
                "test@example.com",
                accountCode,
                openingBalance,
                LocalDate.of(2026, 1, 1),
                "Kadikoy",
                null,
                "TR123456789012345678901234",
                Currency.TRY,
                new BigDecimal("10000.00"),
                role,
                CustomerStatus.ACTIVE,
                "A Grubu"
        );
    }

    private Customer customer(Long id, String name, CustomerRole role) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setCompany(company);
        customer.setName(name);
        customer.setEmail("test@example.com");
        customer.setTaxNumber("1234567890");
        customer.setAddress("Adres");
        customer.setCity("Istanbul");
        customer.setPhoneNumber("5321234567");
        customer.setType(CustomerType.CORPORATE);
        customer.setAccountCode(role == CustomerRole.SELLER ? "320.001" : "120.001");
        customer.setOpeningBalance(BigDecimal.ZERO);
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        customer.setCurrency(Currency.TRY);
        customer.setCreditLimit(new BigDecimal("10000.00"));
        customer.setCustomerRole(role);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCustomerGroup("A Grubu");
        customer.setDeleted(false);
        return customer;
    }

    private Invoice invoice(Long id, Long customerId, InvoiceType type, BigDecimal total, PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("FTR-" + id);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 10));
        invoice.setDueDate(LocalDate.of(2026, 1, 20));
        invoice.setPaymentStatus(status);
        invoice.setTotalAmount(total);
        invoice.setCancelled(false);
        invoice.setDeleted(false);
        return invoice;
    }

    private InvoicePayment payment(Long id, Long invoiceId, BigDecimal amount, LocalDate date) {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(id);
        payment.setCompany(company);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setPaymentDate(date);
        payment.setDeleted(false);
        return payment;
    }

    private CustomerNote note(Long id, Long customerId, String content) {
        CustomerNote note = new CustomerNote();
        note.setNoteId(id);
        note.setCompany(company);
        note.setCustomerId(customerId);
        note.setContent(content);
        return note;
    }
}
