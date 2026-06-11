package com.MuhasebePlus.demo.invoice.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
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
    List<Invoice> findByDueDateBeforeAndPaymentStatusInAndCompanyCompanyIdAndIsDeletedFalse(LocalDate date, List<PaymentStatus> statuses, Long companyId);

    List<Invoice> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    boolean existsByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(Long customerId, Long companyId);

    @Query("SELECT i.customerId, COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.invoiceType = :saleType AND i.paymentStatus <> :paidStatus " +
           "GROUP BY i.customerId")
    List<Object[]> calculateOutstandingBalancesByCompany(
        @Param("companyId") Long companyId,
        @Param("saleType") InvoiceType saleType,
        @Param("paidStatus") PaymentStatus paidStatus);

    @Query("SELECT i.customerId, COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.invoiceType = :purchaseType AND i.paymentStatus <> :paidStatus " +
           "GROUP BY i.customerId")
    List<Object[]> calculateOutstandingBalancesByCompanyPurchase(
        @Param("companyId") Long companyId,
        @Param("purchaseType") InvoiceType purchaseType,
        @Param("paidStatus") PaymentStatus paidStatus);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId " +
           "AND i.isDeleted = false AND i.invoiceType = :saleType AND i.paymentStatus <> :paidStatus")
    BigDecimal calculateOutstandingBalanceForCustomer(
        @Param("customerId") Long customerId,
        @Param("companyId") Long companyId,
        @Param("saleType") InvoiceType saleType,
        @Param("paidStatus") PaymentStatus paidStatus);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId " +
           "AND i.isDeleted = false AND i.invoiceType = :purchaseType AND i.paymentStatus <> :paidStatus")
    BigDecimal calculateOutstandingBalanceForCustomerPurchase(
        @Param("customerId") Long customerId,
        @Param("companyId") Long companyId,
        @Param("purchaseType") InvoiceType purchaseType,
        @Param("paidStatus") PaymentStatus paidStatus);

    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false",
           countQuery = "SELECT COUNT(i) FROM Invoice i WHERE i.company.companyId = :companyId AND i.isDeleted = false")
    Page<Invoice> findAllActiveWithCustomerPage(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false")
    List<Invoice> findAllActiveWithCustomer(@Param("companyId") Long companyId);

    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId AND i.isDeleted = false",
           countQuery = "SELECT COUNT(i) FROM Invoice i WHERE i.customerId = :customerId AND i.company.companyId = :companyId AND i.isDeleted = false")
    Page<Invoice> findByCustomerIdWithCustomerPage(@Param("customerId") Long customerId,
                                                    @Param("companyId") Long companyId,
                                                    Pageable pageable);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId AND i.isDeleted = false")
    List<Invoice> findByCustomerIdWithCustomer(@Param("customerId") Long customerId,
                                                @Param("companyId") Long companyId);

    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type) " +
           "AND (CAST(:startDate AS DATE) IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR i.invoiceDate <= :endDate)",
           countQuery = "SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type) " +
           "AND (CAST(:startDate AS DATE) IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR i.invoiceDate <= :endDate)")
    Page<Invoice> findByFiltersWithCustomerPage(@Param("companyId") Long companyId,
                                                 @Param("status") PaymentStatus status,
                                                 @Param("type") InvoiceType type,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 Pageable pageable);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type)")
    List<Invoice> findByFiltersWithCustomer(@Param("companyId") Long companyId,
                                             @Param("status") PaymentStatus status,
                                             @Param("type") InvoiceType type);

    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Invoice> searchInvoices(@Param("companyId") Long companyId,
                                  @Param("q") String query,
                                  Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.invoiceType = :type " +
           "AND i.dueDate >= :startDate AND i.dueDate <= :endDate " +
           "AND i.company.companyId = :companyId AND i.isDeleted = false")
    BigDecimal sumByTypeAndDateRange(
            @Param("type") InvoiceType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("companyId") Long companyId);

    @Query("SELECT i.customerId, i.customer.name, COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.invoiceType = com.MuhasebePlus.demo.invoice.entity.InvoiceType.sale " +
           "GROUP BY i.customerId, i.customer.name ORDER BY SUM(i.totalAmount) DESC")
    List<Object[]> findTopCustomersByRevenue(@Param("companyId") Long companyId);

    @Query("SELECT DISTINCT i.customerId FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.paymentStatus = com.MuhasebePlus.demo.invoice.entity.PaymentStatus.overdue")
    List<Long> findCustomerIdsWithOverdueInvoices(@Param("companyId") Long companyId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Invoice i " +
           "WHERE i.customerId = :customerId AND i.company.companyId = :companyId " +
           "AND i.isDeleted = false AND i.paymentStatus = com.MuhasebePlus.demo.invoice.entity.PaymentStatus.overdue")
    boolean existsOverdueByCustomerIdAndCompany(@Param("customerId") Long customerId,
                                                 @Param("companyId") Long companyId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.invoiceType = :type " +
           "AND i.paymentStatus = com.MuhasebePlus.demo.invoice.entity.PaymentStatus.paid " +
           "AND i.isDeleted = false AND i.createdAt >= :start AND i.createdAt < :end")
    BigDecimal sumPaidByTypeAndCreatedAtRange(@Param("companyId") Long companyId,
                                               @Param("type") InvoiceType type,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i WHERE i.company.companyId = :companyId " +
           "AND i.invoiceType = :type AND i.isDeleted = false AND i.cancelled = false " +
           "AND i.invoiceDate >= :start AND i.invoiceDate <= :end")
    List<Invoice> findForPeriod(@Param("companyId") Long companyId,
                                 @Param("type") InvoiceType type,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);

    // Public paylaşım linki sorguları: taslak faturalar kesinleşmiş belge olmadığı için her zaman hariç
    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.paymentStatus <> com.MuhasebePlus.demo.invoice.entity.PaymentStatus.draft " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type) " +
           "AND (CAST(:startDate AS DATE) IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR i.invoiceDate <= :endDate)",
           countQuery = "SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.paymentStatus <> com.MuhasebePlus.demo.invoice.entity.PaymentStatus.draft " +
           "AND (:status IS NULL OR i.paymentStatus = :status) " +
           "AND (:type IS NULL OR i.invoiceType = :type) " +
           "AND (CAST(:startDate AS DATE) IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR i.invoiceDate <= :endDate)")
    Page<Invoice> findPublicShareInvoices(@Param("companyId") Long companyId,
                                           @Param("status") PaymentStatus status,
                                           @Param("type") InvoiceType type,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           Pageable pageable);

    @Query(value = "SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.customer " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.paymentStatus <> com.MuhasebePlus.demo.invoice.entity.PaymentStatus.draft " +
           "AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false " +
           "AND i.paymentStatus <> com.MuhasebePlus.demo.invoice.entity.PaymentStatus.draft " +
           "AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Invoice> searchPublicShareInvoices(@Param("companyId") Long companyId,
                                             @Param("q") String query,
                                             Pageable pageable);

    @Query("SELECT i.invoiceType, COUNT(i), COALESCE(SUM(i.subtotal), 0), " +
           "COALESCE(SUM(i.vatAmount), 0), COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.companyId = :companyId AND i.isDeleted = false AND i.cancelled = false " +
           "AND i.paymentStatus <> com.MuhasebePlus.demo.invoice.entity.PaymentStatus.draft " +
           "AND (CAST(:startDate AS DATE) IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR i.invoiceDate <= :endDate) " +
           "GROUP BY i.invoiceType")
    List<Object[]> summarizePublicShareByType(@Param("companyId") Long companyId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}
