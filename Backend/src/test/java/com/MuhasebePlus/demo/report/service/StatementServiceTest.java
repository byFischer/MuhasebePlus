package com.MuhasebePlus.demo.report.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.dto.response.StatementResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatementServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoicePaymentRepository paymentRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private StatementService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long CUSTOMER_ID = 7L;

    private Company company;
    private Customer customer;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setCompany(company);
        customer.setName("Ada Ltd.");
        customer.setTaxNumber("1234567890");
        customer.setAccountCode("120.01.001");
        customer.setOpeningBalance(new BigDecimal("1000.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(customer));
    }

    // Rejects statement generation when the customer is not active for the current company.
    @Test
    void generateStatement_whenCustomerMissing_throwsRuntimeException() {
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateStatement(99L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    // Calculates opening balance from customer opening, prior sales, and prior collections.
    @Test
    void generateStatement_whenSalesPurchasesAndPaymentsExist_calculatesRunningBalance() {
        Invoice priorSale = invoice(1L, InvoiceType.sale, "S-OLD", "300.00", LocalDate.of(2026, 4, 20), PaymentStatus.paid);
        Invoice sale = invoice(2L, InvoiceType.sale, "S-1", "500.00", LocalDate.of(2026, 5, 5), PaymentStatus.pending);
        Invoice purchase = invoice(3L, InvoiceType.purchase, "P-1", "150.00", LocalDate.of(2026, 5, 6), PaymentStatus.pending);
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of(priorSale, sale, purchase));
        when(paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(payment(10L, 1L, "100.00", LocalDate.of(2026, 4, 25))));
        when(paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(2L, COMPANY_ID))
                .thenReturn(List.of(payment(11L, 2L, "200.00", LocalDate.of(2026, 5, 10))));
        when(paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(3L, COMPANY_ID))
                .thenReturn(List.of(payment(12L, 3L, "50.00", LocalDate.of(2026, 5, 11))));

        StatementResponseDto result = service.generateStatement(
                CUSTOMER_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result.openingBalance()).isEqualByComparingTo("1200.00");
        assertThat(result.totalDebit()).isEqualByComparingTo("550.00");
        assertThat(result.totalCredit()).isEqualByComparingTo("350.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("1400.00");
        assertThat(result.lines()).extracting(line -> line.documentNumber())
                .containsExactly("S-1", "P-1", "#PMT11", "#PMT12");
    }

    // Adds customer opening balance as a debit event when the opening date is inside the report range.
    @Test
    void generateStatement_whenOpeningDateInRange_addsOpeningBalanceLine() {
        customer.setOpeningBalanceDate(LocalDate.of(2026, 5, 1));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of());

        StatementResponseDto result = service.generateStatement(
                CUSTOMER_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result.openingBalance()).isEqualByComparingTo("0.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.description()).isEqualTo("Acilis Bakiyesi");
            assertThat(line.debit()).isEqualByComparingTo("1000.00");
            assertThat(line.credit()).isEqualByComparingTo("0.00");
        });
    }

    // Records negative opening balance as a credit event.
    @Test
    void generateStatement_whenNegativeOpeningDateInRange_addsCreditOpeningLine() {
        customer.setOpeningBalance(new BigDecimal("-250.00"));
        customer.setOpeningBalanceDate(LocalDate.of(2026, 5, 1));
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of());

        StatementResponseDto result = service.generateStatement(
                CUSTOMER_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result.closingBalance()).isEqualByComparingTo("-250.00");
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.debit()).isEqualByComparingTo("0.00");
            assertThat(line.credit()).isEqualByComparingTo("250.00");
        });
    }

    // Ignores cancelled and draft invoices so they do not affect customer statement balances.
    @Test
    void generateStatement_whenInvoiceCancelledOrDraft_ignoresAccountingEffect() {
        Invoice cancelled = invoice(1L, InvoiceType.sale, "S-CAN", "300.00", LocalDate.of(2026, 5, 5), PaymentStatus.pending);
        cancelled.setCancelled(true);
        Invoice draft = invoice(2L, InvoiceType.sale, "S-DRF", "500.00", LocalDate.of(2026, 5, 6), PaymentStatus.draft);
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of(cancelled, draft));

        StatementResponseDto result = service.generateStatement(
                CUSTOMER_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result.openingBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.totalDebit()).isEqualByComparingTo("0.00");
        assertThat(result.totalCredit()).isEqualByComparingTo("0.00");
        assertThat(result.lines()).isEmpty();
    }

    // Uses customer opening balance directly when no start date is supplied.
    @Test
    void generateStatement_whenStartDateNull_usesCustomerOpeningBalance() {
        customer.setOpeningBalanceDate(null);
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of());

        StatementResponseDto result = service.generateStatement(CUSTOMER_ID, null, LocalDate.of(2026, 5, 31));

        assertThat(result.openingBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("1000.00");
    }

    private Invoice invoice(Long id, InvoiceType type, String number, String amount, LocalDate date, PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setInvoiceType(type);
        invoice.setInvoiceNumber(number);
        invoice.setInvoiceDate(date);
        invoice.setPaymentStatus(status);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setDeleted(false);
        invoice.setCancelled(false);
        return invoice;
    }

    private InvoicePayment payment(Long id, Long invoiceId, String amount, LocalDate date) {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(id);
        payment.setCompany(company);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(date);
        payment.setPaymentMethod(PaymentMethod.cash);
        payment.setBankAccountId(1L);
        payment.setDeleted(false);
        return payment;
    }
}
