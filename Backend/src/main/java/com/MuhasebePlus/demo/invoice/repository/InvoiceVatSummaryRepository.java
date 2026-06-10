package com.MuhasebePlus.demo.invoice.repository;

import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceVatSummaryRepository extends JpaRepository<InvoiceVatSummary, Long> {

    List<InvoiceVatSummary> findByInvoiceIdOrderByVatRateAsc(Long invoiceId);

    @Modifying
    @Query("DELETE FROM InvoiceVatSummary s WHERE s.invoiceId = :invoiceId")
    void deleteByInvoiceId(Long invoiceId);

    @Query("""
            SELECT s FROM InvoiceVatSummary s
            JOIN Invoice i ON i.invoiceId = s.invoiceId
            WHERE i.company.companyId = :companyId
              AND i.invoiceType = 'sale'
              AND i.isDeleted = false
              AND i.cancelled = false
              AND FUNCTION('YEAR', i.invoiceDate) = :year
              AND FUNCTION('MONTH', i.invoiceDate) = :month
            """)
    List<InvoiceVatSummary> findSalesVatSummaryForPeriod(Long companyId, int year, int month);

    @Query("""
            SELECT s FROM InvoiceVatSummary s
            JOIN Invoice i ON i.invoiceId = s.invoiceId
            WHERE i.company.companyId = :companyId
              AND i.invoiceType = 'purchase'
              AND i.isDeleted = false
              AND i.cancelled = false
              AND FUNCTION('YEAR', i.invoiceDate) = :year
              AND FUNCTION('MONTH', i.invoiceDate) = :month
            """)
    List<InvoiceVatSummary> findPurchaseVatSummaryForPeriod(Long companyId, int year, int month);

    @Query("""
            SELECT s FROM InvoiceVatSummary s
            JOIN Invoice i ON i.invoiceId = s.invoiceId
            WHERE i.company.companyId = :companyId
              AND i.isDeleted = false
              AND i.cancelled = false
              AND s.withholdingTaxCodeId IS NOT NULL
              AND FUNCTION('YEAR', i.invoiceDate) = :year
              AND FUNCTION('MONTH', i.invoiceDate) = :month
            """)
    List<InvoiceVatSummary> findWithholdingsForPeriod(Long companyId, int year, int month);

    @Query("""
            SELECT s FROM InvoiceVatSummary s
            JOIN Invoice i ON i.invoiceId = s.invoiceId
            WHERE i.company.companyId = :companyId
              AND i.invoiceType = 'purchase'
              AND i.isDeleted = false
              AND i.cancelled = false
              AND s.withholdingTaxCodeId IS NOT NULL
              AND FUNCTION('YEAR', i.invoiceDate) = :year
              AND FUNCTION('MONTH', i.invoiceDate) = :month
            """)
    List<InvoiceVatSummary> findPurchaseWithholdingsForPeriod(Long companyId, int year, int month);
}
