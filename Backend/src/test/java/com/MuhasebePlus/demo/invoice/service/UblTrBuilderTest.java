package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.DiscountType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.stock.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UblTrBuilderTest {

    @Test
    void build_whenSaleInvoiceHasCustomerProductsDiscountAndExchangeRate_writesUblTrXml() {
        Company company = company();
        Customer customer = customer(CustomerType.INDIVIDUAL);
        Invoice invoice = invoice(InvoiceType.sale);
        InvoiceLineItem first = line(10, 2, "100.00", "10.00", "20.00");
        InvoiceLineItem second = line(20, 1, "50.00", null, BigDecimal.ZERO.toString());
        Product product = Product.builder().productId(10).name("Bakim & Destek <A>").build();

        String xml = UblTrBuilder.build(invoice, company, customer, List.of(first, second), Map.of(10, product));

        assertThat(xml).startsWith("<?xml");
        assertThat(xml).contains("<cbc:InvoiceTypeCode>SATIS</cbc:InvoiceTypeCode>");
        assertThat(xml).contains("<cbc:DocumentCurrencyCode>USD</cbc:DocumentCurrencyCode>");
        assertThat(xml).contains("<cbc:CalculationRate>32.50</cbc:CalculationRate>");
        assertThat(xml).contains("Bakim &amp; Destek &lt;A&gt;");
        assertThat(xml).contains("schemeID=\"TCKN\"");
        assertThat(xml).contains("<cbc:AllowanceTotalAmount currencyID=\"USD\">15.00</cbc:AllowanceTotalAmount>");
        assertThat(xml).contains("<cbc:Percent>20</cbc:Percent>");
        assertThat(xml).contains("<cbc:Percent>0</cbc:Percent>");
        assertThat(xml).contains("<cbc:PayableAmount currencyID=\"USD\">231.00</cbc:PayableAmount>");
        assertThat(xml).contains("<cbc:Name>#20</cbc:Name>");
    }

    @Test
    void build_whenPurchaseInvoiceHasMinimalData_usesDefaultsAndOmitsOptionalCustomer() {
        Company company = company();
        Invoice invoice = invoice(InvoiceType.purchase);
        invoice.setInvoiceDate(null);
        invoice.setDueDate(null);
        invoice.setCurrency(null);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDescription(null);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        InvoiceLineItem line = line(10, 1, "100.00", null, null);

        String xml = UblTrBuilder.build(invoice, company, null, List.of(line), Map.of());

        assertThat(xml).contains("<cbc:InvoiceTypeCode>ALIS</cbc:InvoiceTypeCode>");
        assertThat(xml).contains("<cbc:DocumentCurrencyCode>TRY</cbc:DocumentCurrencyCode>");
        assertThat(xml).doesNotContain("AccountingCustomerParty");
        assertThat(xml).doesNotContain("TaxExchangeRate");
        assertThat(xml).doesNotContain("AllowanceCharge");
        assertThat(xml).contains("<cbc:TaxAmount currencyID=\"TRY\">0.00</cbc:TaxAmount>");
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyName("Test & Co");
        company.setTaxNumber("1234567890");
        company.setTaxOffice("Kadikoy");
        company.setAddress("Adres <Merkez>");
        company.setCity("Istanbul");
        company.setPhone("02121234567");
        company.setEmail("info@test.com");
        return company;
    }

    private Customer customer(CustomerType type) {
        Customer customer = new Customer();
        customer.setName("Ahmet Musteri");
        customer.setType(type);
        customer.setTaxNumber("11111111111");
        customer.setAddress("Musteri Adresi");
        customer.setCity("Ankara");
        return customer;
    }

    private Invoice invoice(InvoiceType type) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FTR-UBL");
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 10));
        invoice.setDueDate(LocalDate.of(2026, 1, 25));
        invoice.setDescription("Aciklama & not");
        invoice.setCurrency(Currency.USD);
        invoice.setExchangeRate(new BigDecimal("32.50"));
        invoice.setDiscountType(DiscountType.AMOUNT);
        invoice.setDiscountAmount(new BigDecimal("15.00"));
        invoice.setTotalAmount(new BigDecimal("231.00"));
        return invoice;
    }

    private InvoiceLineItem line(Integer productId, int quantity, String unitPrice, String discountRate, String vatRate) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setDiscountRate(discountRate != null ? new BigDecimal(discountRate) : null);
        line.setVatRate(vatRate != null ? new BigDecimal(vatRate) : null);
        return line;
    }
}
