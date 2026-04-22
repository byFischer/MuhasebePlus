package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Integer> {

    List<InvoiceLineItem> findByInvoiceIdAndIsDeletedFalse(Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);

    boolean existsByProductIdAndIsDeletedFalse(Integer productId);
}
