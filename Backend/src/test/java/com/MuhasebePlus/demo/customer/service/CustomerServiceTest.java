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
import com.MuhasebePlus.demo.customer.dto.response.ImportResultDto;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import static org.mockito.Mockito.times;
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

    @Test
    void createCustomer_whenAccountCodeExists_throwsAndDoesNotSave() {
        CustomerRequestDto dto = request("ABC Ltd", "1234567890", CustomerRole.BUYER, BigDecimal.ZERO, " 120.001 ");
        when(customerRepository.existsByAccountCodeAndCompanyCompanyIdAndIsDeletedFalse("120.001", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("account code");

        verify(customerRepository, never()).save(any());
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

    @Test
    void getCustomerById_calculatesBalanceAndOverdueFlagWithinCompany() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setOpeningBalance(new BigDecimal("50.00"));
        Invoice sale = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("100.00"), PaymentStatus.pending);
        Invoice purchase = invoice(2L, 10L, InvoiceType.purchase, new BigDecimal("30.00"), PaymentStatus.pending);
        Invoice draft = invoice(3L, 10L, InvoiceType.sale, new BigDecimal("999.00"), PaymentStatus.draft);
        Invoice cancelled = invoice(4L, 10L, InvoiceType.sale, new BigDecimal("999.00"), PaymentStatus.pending);
        cancelled.setCancelled(true);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of(sale, purchase, draft, cancelled));
        when(invoicePaymentRepository.sumAmountByInvoiceId(1L)).thenReturn(Optional.of(new BigDecimal("20.00")));
        when(invoicePaymentRepository.sumAmountByInvoiceId(2L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(invoiceRepository.existsOverdueByCustomerIdAndCompany(10L, COMPANY_ID)).thenReturn(true);

        CustomerResponseDto result = service.getCustomerById(10L);

        assertThat(result.currentBalance()).isEqualByComparingTo("100.00");
        assertThat(result.hasOverdueInvoices()).isTrue();
    }

    @Test
    void getCustomerById_whenCustomerBelongsToAnotherCompany_throwsAccessDenied() {
        Company otherCompany = new Company();
        otherCompany.setCompanyId(99L);
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setCompany(otherCompany);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.getCustomerById(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz yok");
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

    @Test
    void updateCustomer_whenOnlyMasterDataChanges_doesNotRecreateOpeningJournal() {
        Customer existing = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        existing.setOpeningBalance(new BigDecimal("100.00"));
        existing.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        existing.setAccountCode("120.001");
        CustomerRequestDto dto = request("ABC Updated", "1234567890", CustomerRole.BUYER,
                new BigDecimal("100.00"), " 120.001 ");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of());

        CustomerResponseDto result = service.updateCustomer(10L, dto);

        assertThat(result.name()).isEqualTo("ABC Updated");
        verify(journalEntryService, never()).reverseForCustomerOpening(anyLong(), anyLong(), any());
        verify(journalEntryService, never()).createForCustomerOpening(any());
    }

    @Test
    void updateCustomer_whenNewTaxNumberAlreadyExists_throws() {
        Customer existing = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        CustomerRequestDto dto = request("ABC Ltd", "9999999999", CustomerRole.BUYER,
                BigDecimal.ZERO, "120.001");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse("9999999999", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateCustomer(10L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tax number");
    }

    @Test
    void updateCustomer_whenNewAccountCodeAlreadyExists_throws() {
        Customer existing = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        existing.setAccountCode("120.001");
        CustomerRequestDto dto = request("ABC Ltd", "1234567890", CustomerRole.BUYER,
                BigDecimal.ZERO, "120.999");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByAccountCodeAndCompanyCompanyIdAndIsDeletedFalse("120.999", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateCustomer(10L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("account code");
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

    @Test
    void searchAndPagedQueries_returnBalancesAndOverdueFlags() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setOpeningBalance(new BigDecimal("10.00"));
        Invoice sale = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("90.00"), PaymentStatus.pending);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);
        when(customerRepository.searchActive(COMPANY_ID, "abc")).thenReturn(List.of(customer));
        when(customerRepository.findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType.INDIVIDUAL, COMPANY_ID))
                .thenReturn(List.of(customer));
        when(customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID, pageable)).thenReturn(page);
        when(customerRepository.searchActivePage(COMPANY_ID, "abc", pageable)).thenReturn(page);
        when(customerRepository.findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType.INDIVIDUAL, COMPANY_ID, pageable))
                .thenReturn(page);
        when(customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID)).thenReturn(List.of(customer));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID)).thenReturn(List.of(sale));
        when(invoicePaymentRepository.sumAmountByInvoiceId(1L)).thenReturn(Optional.of(new BigDecimal("20.00")));
        when(invoiceRepository.findCustomerIdsWithOverdueInvoices(COMPANY_ID)).thenReturn(List.of(10L));

        List<CustomerResponseDto> search = service.searchCustomers("abc");
        List<CustomerResponseDto> typed = service.getCustomersByType("INDIVIDUAL");
        Page<CustomerResponseDto> allPage = service.getAllCustomersPaged(pageable);
        Page<CustomerResponseDto> searchPage = service.searchCustomersPaged("abc", pageable);
        Page<CustomerResponseDto> typePage = service.getCustomersByTypePaged("INDIVIDUAL", pageable);

        assertThat(search.get(0).currentBalance()).isEqualByComparingTo("80.00");
        assertThat(typed.get(0).hasOverdueInvoices()).isTrue();
        assertThat(allPage.getContent()).hasSize(1);
        assertThat(searchPage.getContent().get(0).customerId()).isEqualTo(10L);
        assertThat(typePage.getContent().get(0).type()).isEqualTo("INDIVIDUAL");
    }

    @Test
    void getCustomerActivity_whenPurchaseAndPriorSaleExist_returnsOpeningAndPaymentLines() {
        Customer customer = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        customer.setOpeningBalance(new BigDecimal("100.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        Invoice priorSale = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("1000.00"), PaymentStatus.pending);
        priorSale.setInvoiceDate(LocalDate.of(2026, 1, 10));
        Invoice purchase = invoice(2L, 10L, InvoiceType.purchase, new BigDecimal("250.00"), PaymentStatus.pending);
        purchase.setInvoiceDate(LocalDate.of(2026, 2, 10));
        InvoicePayment priorPayment = payment(80L, 1L, new BigDecimal("400.00"), LocalDate.of(2026, 1, 20));
        InvoicePayment supplierPayment = payment(81L, 2L, new BigDecimal("50.00"), LocalDate.of(2026, 2, 12));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(List.of(priorSale, purchase));
        when(invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(priorPayment));
        when(invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(2L, COMPANY_ID))
                .thenReturn(List.of(supplierPayment));

        List<CustomerActivityDto> result = service.getCustomerActivity(
                10L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThat(result).extracting(CustomerActivityDto::type)
                .containsExactly("ACILIS", "FATURA", "ODEME");
        assertThat(result.get(0).balance()).isEqualByComparingTo("700.00");
        assertThat(result.get(2).balance()).isEqualByComparingTo("500.00");
    }

    @Test
    void getCustomerAging_groupsOpenReceivablesIntoAllBucketsAndSortsByTotal() {
        Customer first = customer(10L, "ABC Ltd", CustomerRole.BUYER);
        Customer second = customer(11L, "XYZ Ltd", CustomerRole.BUYER);
        Invoice bucket0to30 = invoice(1L, 10L, InvoiceType.sale, new BigDecimal("100.00"), PaymentStatus.pending);
        bucket0to30.setDueDate(LocalDate.now().minusDays(10));
        Invoice bucket61to90 = invoice(2L, 10L, InvoiceType.sale, new BigDecimal("200.00"), PaymentStatus.pending);
        bucket61to90.setDueDate(LocalDate.now().minusDays(70));
        Invoice bucket90plus = invoice(3L, 11L, InvoiceType.sale, new BigDecimal("300.00"), PaymentStatus.pending);
        bucket90plus.setDueDate(LocalDate.now().minusDays(120));
        Invoice future = invoice(4L, 10L, InvoiceType.sale, new BigDecimal("999.00"), PaymentStatus.pending);
        future.setDueDate(LocalDate.now().plusDays(1));
        Invoice missingCustomer = invoice(5L, 99L, InvoiceType.sale, new BigDecimal("999.00"), PaymentStatus.pending);
        missingCustomer.setDueDate(LocalDate.now().minusDays(20));
        Invoice fullyPaid = invoice(6L, 11L, InvoiceType.sale, new BigDecimal("50.00"), PaymentStatus.pending);
        fullyPaid.setDueDate(LocalDate.now().minusDays(20));
        when(invoiceRepository.findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of(bucket0to30, bucket61to90, bucket90plus, future, missingCustomer, fullyPaid));
        when(customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(first, second));
        when(invoicePaymentRepository.sumAmountByInvoiceId(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(invoicePaymentRepository.sumAmountByInvoiceId(2L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(invoicePaymentRepository.sumAmountByInvoiceId(3L)).thenReturn(Optional.of(new BigDecimal("50.00")));
        when(invoicePaymentRepository.sumAmountByInvoiceId(6L)).thenReturn(Optional.of(new BigDecimal("50.00")));

        List<CustomerAgingDto> result = service.getCustomerAging();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).customerId()).isEqualTo(10L);
        assertThat(result.get(0).bucket0to30()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).bucket61to90()).isEqualByComparingTo("200.00");
        assertThat(result.get(1).bucket90plus()).isEqualByComparingTo("250.00");
    }

    @Test
    void getNotesByCustomerId_whenCustomerExists_returnsNotesInRepositoryOrder() {
        CustomerNote first = note(1L, 10L, "Ilk not");
        CustomerNote second = note(2L, 10L, "Ikinci not");
        when(customerRepository.existsByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(true);
        when(customerNoteRepository.findByCustomerIdAndCompanyCompanyIdOrderByCreatedAtDesc(10L, COMPANY_ID))
                .thenReturn(List.of(first, second));

        List<CustomerNoteResponseDto> result = service.getNotesByCustomerId(10L);

        assertThat(result).extracting(CustomerNoteResponseDto::content)
                .containsExactly("Ilk not", "Ikinci not");
    }

    @Test
    void addNote_whenCustomerDoesNotBelongToCompany_throws() {
        when(customerRepository.existsByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.addNote(10L, new CustomerNoteRequestDto("Not")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("access denied");

        verify(customerNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_whenNoteBelongsToAnotherCompany_throws() {
        Company otherCompany = new Company();
        otherCompany.setCompanyId(99L);
        CustomerNote note = note(55L, 10L, "Not");
        note.setCompany(otherCompany);
        when(customerNoteRepository.findById(55L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> service.updateNote(55L, new CustomerNoteRequestDto("Yeni")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz yok");
    }

    @Test
    void importCustomers_whenCsvContainsValidAndInvalidRows_returnsMixedResult() {
        String csv = String.join("\n",
                "name,tax,address,city,phone,type,email,account,opening,date,taxOffice,identity,iban,currency,limit,role,group",
                "CSV Ltd,1111111111,Adres,Istanbul,5321234567,individual,csv@example.com, 120.777 ,123.45,2026-01-05,Kadikoy,11111111111,TR123,try,1000,seller,Grup",
                "Bad Ltd,2222222222,Adres,Istanbul,5321234567,CORPORATE,bad@example.com,120.888,not-number,2026-01-05,Kadikoy,22222222222,TR124,TRY,1000,BUYER,Grup");
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            customer.setCustomerId(70L);
            return customer;
        });

        ImportResultDto result = service.importCustomers(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors().get(0)).contains("Line 3");
        verify(customerRepository, times(1)).save(any());
    }

    @Test
    void importCustomers_whenExcelFileContainsCustomer_parsesAndCreatesCustomer() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Customers");
            sheet.createRow(0);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Excel Ltd");
            row.createCell(1).setCellValue("3333333333");
            row.createCell(2).setCellValue("Adres");
            row.createCell(3).setCellValue("Ankara");
            row.createCell(4).setCellValue("5321234567");
            row.createCell(5).setCellValue("CORPORATE");
            row.createCell(6).setCellValue("excel@example.com");
            row.createCell(7).setCellValue("320.777");
            row.createCell(8).setCellValue(250.75);
            row.createCell(9).setCellValue(LocalDateTime.of(2026, 2, 1, 0, 0));
            row.createCell(10).setCellValue("Cankaya");
            row.createCell(11).setCellValue("33333333333");
            row.createCell(12).setCellValue("TR333");
            row.createCell(13).setCellValue("TRY");
            row.createCell(14).setCellValue(5000.00);
            row.createCell(15).setCellValue("BUYER");
            row.createCell(16).setCellValue("A Grubu");
            workbook.write(out);
        }
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            customer.setCustomerId(71L);
            return customer;
        });

        ImportResultDto result = service.importCustomers(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isZero();
    }

    @Test
    void importCustomers_whenFormatUnsupported_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "customers.json",
                "application/json", "{}".getBytes(StandardCharsets.UTF_8));

        ImportResultDto result = service.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors().get(0)).contains("Unsupported file format");
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
