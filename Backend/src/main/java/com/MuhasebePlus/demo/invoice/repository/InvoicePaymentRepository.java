package com.MuhasebePlus.demo.invoice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    List<InvoicePayment> findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(Long invoiceId, Long companyId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM InvoicePayment p WHERE p.invoiceId = :invoiceId AND p.isDeleted = false")
    Optional<BigDecimal> sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    Optional<InvoicePayment> findByPaymentIdAndCompanyCompanyId(Long paymentId, Long companyId);

    List<InvoicePayment> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
