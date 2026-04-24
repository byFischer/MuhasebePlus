package com.MuhasebePlus.demo.invoice.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Invoice> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);
    List<Invoice> findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(Long customerId, Long companyId);
    List<Invoice> findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(PaymentStatus paymentStatus, Long companyId);
    List<Invoice> findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType invoiceType, Long companyId);
    List<Invoice> findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(PaymentStatus paymentStatus, InvoiceType invoiceType, Long companyId);

    List<Invoice> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false")
    List<Invoice> findAllActiveWithCustomer(@Param("companyId") Long companyId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId AND i.isDeleted = false")
    List<Invoice> findByCustomerIdWithCustomer(@Param("customerId") Long customerId,
                                               @Param("companyId") Long companyId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type)")
    List<Invoice> findByFiltersWithCustomer(@Param("companyId") Long companyId,
                                            @Param("status") PaymentStatus status,
                                            @Param("type") InvoiceType type);
}
