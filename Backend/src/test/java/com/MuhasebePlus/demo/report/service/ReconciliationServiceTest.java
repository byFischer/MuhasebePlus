package com.MuhasebePlus.demo.report.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.dto.response.ReconciliationResponseDto;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoicePaymentRepository paymentRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private ReconciliationService service;

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

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(customer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
    }

    // Rejects reconciliation generation when the customer is missing.
    @Test
    void generateReconciliation_whenCustomerMissing_throwsRuntimeException() {
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateReconciliation(99L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    // Rejects reconciliation generation when company context cannot be resolved.
    @Test
    void generateReconciliation_whenCompanyMissing_throwsRuntimeException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateReconciliation(CUSTOMER_ID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found");
    }

    // Totals in-range sale invoices, collected amounts, and remaining open items.
    @Test
    void generateReconciliation_whenSaleInvoicesExist_calculatesOpenItemsAndTotals() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        Invoice openSale = invoice(1L, InvoiceType.sale, "S-1", "1000.00", LocalDate.of(2026, 5, 5));
        openSale.setDueDate(LocalDate.now().minusDays(3));
        Invoice fullyPaidSale = invoice(2L, InvoiceType.sale, "S-2", "200.00", LocalDate.of(2026, 5, 8));
        Invoice purchase = invoice(3L, InvoiceType.purchase, "P-1", "700.00", LocalDate.of(2026, 5, 9));
        Invoice outsideRange = invoice(4L, InvoiceType.sale, "S-OLD", "500.00", LocalDate.of(2026, 4, 30));
        Invoice cancelled = invoice(5L, InvoiceType.sale, "S-CAN", "300.00", LocalDate.of(2026, 5, 10));
        cancelled.setCancelled(true);
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of(openSale, fullyPaidSale, purchase, outsideRange, cancelled));
        when(paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(payment(10L, 1L, "400.00")));
        when(paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(2L, COMPANY_ID))
                .thenReturn(List.of(payment(11L, 2L, "200.00")));

        ReconciliationResponseDto result = service.generateReconciliation(CUSTOMER_ID, start, end);

        assertThat(result.companyTaxNumber()).isEqualTo("");
        assertThat(result.totalInvoiced()).isEqualByComparingTo("1200.00");
        assertThat(result.totalCollected()).isEqualByComparingTo("600.00");
        assertThat(result.closingBalance()).isEqualByComparingTo("600.00");
        assertThat(result.openItems()).singleElement().satisfies(item -> {
            assertThat(item.invoiceNumber()).isEqualTo("S-1");
            assertThat(item.paidAmount()).isEqualByComparingTo("400.00");
            assertThat(item.remainingAmount()).isEqualByComparingTo("600.00");
            assertThat(item.daysOverdue()).isEqualTo(ChronoUnit.DAYS.between(openSale.getDueDate(), LocalDate.now()));
        });
    }

    // Uses company tax number when it exists.
    @Test
    void generateReconciliation_whenCompanyTaxNumberExists_mapsIt() {
        company.setTaxNumber("9876543210");
        when(invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(List.of());

        ReconciliationResponseDto result = service.generateReconciliation(
                CUSTOMER_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result.companyTaxNumber()).isEqualTo("9876543210");
        assertThat(result.openItems()).isEmpty();
    }

    private Invoice invoice(Long id, InvoiceType type, String number, String amount, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setInvoiceType(type);
        invoice.setInvoiceNumber(number);
        invoice.setInvoiceDate(date);
        invoice.setDueDate(date.plusDays(10));
        invoice.setPaymentStatus(PaymentStatus.pending);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setCancelled(false);
        invoice.setDeleted(false);
        return invoice;
    }

    private InvoicePayment payment(Long id, Long invoiceId, String amount) {
        InvoicePayment payment = new InvoicePayment();
        payment.setPaymentId(id);
        payment.setCompany(company);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDate.of(2026, 5, 20));
        payment.setPaymentMethod(PaymentMethod.cash);
        payment.setBankAccountId(1L);
        payment.setDeleted(false);
        return payment;
    }
}
