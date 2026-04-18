package com.MuhasebePlus.demo.invoice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class InvoiceRepositoryTest {

    @Autowired
    InvoiceRepository invoiceRepository;

    private Invoice sampleInvoice;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();

        sampleInvoice = new Invoice();
        sampleInvoice.setInvoiceNumber("INV-001");
        sampleInvoice.setCustomerId(10L);
        sampleInvoice.setInvoiceType(InvoiceType.sale);
        sampleInvoice.setDueDate(LocalDate.of(2026, 6, 1));
        sampleInvoice.setPaymentStatus(PaymentStatus.pending);
        sampleInvoice.setSubtotal(new BigDecimal("100.00"));
        sampleInvoice.setVatAmount(new BigDecimal("20.00"));
        sampleInvoice.setTotalAmount(new BigDecimal("120.00"));
        invoiceRepository.save(sampleInvoice);
    }

    @Test
    void findByInvoiceNumber_Exists_ShouldReturnInvoice() {
        assertThat(invoiceRepository.findByInvoiceNumber("INV-001")).isPresent();
    }

    @Test
    void findByInvoiceNumber_NotExists_ShouldReturnEmpty() {
        assertThat(invoiceRepository.findByInvoiceNumber("INV-999")).isEmpty();
    }

    @Test
    void existsByInvoiceNumber_Exists_ShouldReturnTrue() {
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-001")).isTrue();
    }

    @Test
    void existsByInvoiceNumber_NotExists_ShouldReturnFalse() {
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-999")).isFalse();
    }

    @Test
    void findByCustomerId_ShouldReturnCorrectInvoices() {
        Invoice second = new Invoice();
        second.setInvoiceNumber("INV-002");
        second.setCustomerId(10L);
        second.setInvoiceType(InvoiceType.purchase);
        second.setDueDate(LocalDate.of(2026, 7, 1));
        second.setPaymentStatus(PaymentStatus.paid);
        second.setSubtotal(new BigDecimal("200.00"));
        second.setVatAmount(new BigDecimal("40.00"));
        second.setTotalAmount(new BigDecimal("240.00"));
        invoiceRepository.save(second);

        assertThat(invoiceRepository.findByCustomerId(10L)).hasSize(2);
        assertThat(invoiceRepository.findByCustomerId(99L)).isEmpty();
    }

    @Test
    void findByPaymentStatus_ShouldReturnFiltered() {
        assertThat(invoiceRepository.findByPaymentStatus(PaymentStatus.pending)).hasSize(1);
        assertThat(invoiceRepository.findByPaymentStatus(PaymentStatus.paid)).isEmpty();
    }

    @Test
    void findByInvoiceType_ShouldReturnFiltered() {
        assertThat(invoiceRepository.findByInvoiceType(InvoiceType.sale)).hasSize(1);
        assertThat(invoiceRepository.findByInvoiceType(InvoiceType.purchase)).isEmpty();
    }

    @Test
    void findByPaymentStatusAndInvoiceType_ShouldReturnFiltered() {
        assertThat(invoiceRepository.findByPaymentStatusAndInvoiceType(PaymentStatus.pending, InvoiceType.sale)).hasSize(1);
        assertThat(invoiceRepository.findByPaymentStatusAndInvoiceType(PaymentStatus.paid, InvoiceType.sale)).isEmpty();
    }

    @Test
    void findByDueDateBefore_ShouldReturnOverdue() {
        assertThat(invoiceRepository.findByDueDateBefore(LocalDate.of(2027, 1, 1))).hasSize(1);
        assertThat(invoiceRepository.findByDueDateBefore(LocalDate.of(2025, 1, 1))).isEmpty();
    }
}
