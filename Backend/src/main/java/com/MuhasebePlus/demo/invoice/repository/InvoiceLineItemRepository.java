package com.MuhasebePlus.demo.invoice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Integer> {

    List<InvoiceLineItem> findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(Long invoiceId, Long companyId);

    void deleteByInvoiceIdAndCompanyCompanyId(Long invoiceId, Long companyId);

    void deleteByInvoiceId(Long invoiceId);

    boolean existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(Integer productId, Long companyId);

    @Query("SELECT li FROM InvoiceLineItem li " +
           "WHERE li.invoiceId IN :invoiceIds AND li.company.companyId = :companyId AND li.isDeleted = false")
    List<InvoiceLineItem> findByInvoiceIdInAndCompanyAndActive(
        @Param("invoiceIds") List<Long> invoiceIds,
        @Param("companyId") Long companyId);

    @Query("SELECT MAX(i.createdAt) FROM InvoiceLineItem li JOIN Invoice i ON li.invoiceId = i.invoiceId " +
           "WHERE li.productId = :productId AND li.company.companyId = :companyId AND li.isDeleted = false AND i.isDeleted = false")
    Optional<LocalDateTime> findLastSaleDateByProductId(@Param("productId") Integer productId, @Param("companyId") Long companyId);

    @Query("SELECT li.productId, SUM(li.quantity) FROM InvoiceLineItem li JOIN Invoice i ON li.invoiceId = i.invoiceId " +
           "WHERE li.company.companyId = :companyId AND li.isDeleted = false AND i.isDeleted = false " +
           "AND i.createdAt >= :since GROUP BY li.productId")
    List<Object[]> sumQuantityByProductLast12Months(@Param("companyId") Long companyId, @Param("since") LocalDateTime since);

    @Query("SELECT li.productId, MAX(i.createdAt) FROM InvoiceLineItem li JOIN Invoice i ON li.invoiceId = i.invoiceId " +
           "WHERE i.company.companyId = :companyId AND li.isDeleted = false AND i.isDeleted = false " +
           "AND li.productId IN :productIds GROUP BY li.productId")
    List<Object[]> findLastSaleDatesByProductIds(@Param("companyId") Long companyId,
                                                  @Param("productIds") List<Integer> productIds);
}
