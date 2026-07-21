package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.DiscountType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfInvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository lineItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;

    @Test
    void generatePdf_writesInvoiceCompanyCustomerLineAndTotals() throws Exception {
        PdfInvoiceService service = new PdfInvoiceService(
                invoiceRepository, lineItemRepository, customerRepository,
                productRepository, companyRepository, companyContext);
        Company company = company();
        Customer customer = customer();
        Invoice invoice = invoice(100L, InvoiceType.sale);
        InvoiceLineItem first = line(1, 10, 2, "100.00", "10.00", "20.00", "5.00", "216.00");
        InvoiceLineItem second = line(2, 20, 1, "50.00", null, null, null, "50.00");
        Product product = Product.builder().productId(10).name("Danismanlik Hizmeti").build();
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(100L, 1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(customer));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, 1L))
                .thenReturn(List.of(first, second));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(10, 1L))
                .thenReturn(Optional.of(product));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(20, 1L))
                .thenReturn(Optional.empty());

        byte[] pdf = service.generatePdf(100L);

        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("SATIS FATURASI");
            assertThat(text).contains("Test A.S.");
            assertThat(text).contains("ABC Ltd");
            assertThat(text).contains("Danismanlik Hizmeti");
            assertThat(text).contains("GENEL TOPLAM");
        }
    }

    @Test
    void generatePdf_whenInvoiceMissing_throws() {
        PdfInvoiceService service = new PdfInvoiceService(
                invoiceRepository, lineItemRepository, customerRepository,
                productRepository, companyRepository, companyContext);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePdf(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invoice not found");
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        company.setTaxOffice("Kadikoy");
        company.setTaxNumber("1234567890");
        company.setAddress("Adres");
        company.setCity("Istanbul");
        company.setPhone("02121234567");
        company.setEmail("info@test.com");
        return company;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setCustomerId(7L);
        customer.setName("ABC Ltd");
        customer.setType(CustomerType.CORPORATE);
        customer.setTaxOffice("Besiktas");
        customer.setTaxNumber("9876543210");
        customer.setAddress("Musteri Adresi");
        customer.setCity("Istanbul");
        return customer;
    }

    private Invoice invoice(Long id, InvoiceType type) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCustomerId(7L);
        invoice.setInvoiceNumber("FTR-100");
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 10));
        invoice.setDueDate(LocalDate.of(2026, 1, 25));
        invoice.setCurrency(Currency.TRY);
        invoice.setSubtotal(new BigDecimal("250.00"));
        invoice.setVatAmount(new BigDecimal("40.00"));
        invoice.setDiscountType(DiscountType.PERCENTAGE);
        invoice.setDiscountAmount(new BigDecimal("10.00"));
        invoice.setWithholdingTaxAmount(new BigDecimal("5.00"));
        invoice.setTotalAmount(new BigDecimal("275.00"));
        return invoice;
    }

    private InvoiceLineItem line(Integer id, Integer productId, int quantity, String unitPrice,
                                 String discountRate, String vatRate, String withholdingRate, String total) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setLineItemId(id);
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setDiscountRate(discountRate != null ? new BigDecimal(discountRate) : BigDecimal.ZERO);
        line.setVatRate(vatRate != null ? new BigDecimal(vatRate) : null);
        line.setWithholdingTaxRate(withholdingRate != null ? new BigDecimal(withholdingRate) : BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal(total));
        return line;
    }
}
