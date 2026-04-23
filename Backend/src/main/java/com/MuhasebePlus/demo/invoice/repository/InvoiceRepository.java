package com.MuhasebePlus.demo.invoice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumberAndCompanyCompanyId(String invoiceNumber, Long companyId);
    
    Optional<Invoice> findByInvoiceIdAndCompanyCompanyId(Long invoiceId, Long companyId);

    boolean existsByInvoiceNumberAndCompanyCompanyId(String invoiceNumber, Long companyId);

    List<Invoice> findByCustomerIdAndCompanyCompanyId(Long customerId, Long companyId);
    List<Invoice> findByPaymentStatusAndCompanyCompanyId(PaymentStatus paymentStatus, Long companyId);
    List<Invoice> findByInvoiceTypeAndCompanyCompanyId(InvoiceType invoiceType, Long companyId);
    List<Invoice> findByPaymentStatusAndInvoiceTypeAndCompanyCompanyId(PaymentStatus paymentStatus, InvoiceType invoiceType, Long companyId);
    List<Invoice> findByDueDateBeforeAndCompanyCompanyId(LocalDate date, Long companyId);
    List<Invoice> findByCompanyCompanyId(Long companyId);
}
