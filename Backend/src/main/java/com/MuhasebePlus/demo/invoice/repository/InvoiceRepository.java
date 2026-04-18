package com.MuhasebePlus.demo.invoice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByCustomerId(Long customerId);
    List<Invoice> findByPaymentStatus(PaymentStatus paymentStatus);
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);
    List<Invoice> findByPaymentStatusAndInvoiceType(PaymentStatus paymentStatus, InvoiceType invoiceType);
    List<Invoice> findByDueDateBefore(LocalDate date);
}
