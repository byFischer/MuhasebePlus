package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;

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
}
