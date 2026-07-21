package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.CollectionPromise;

public interface CollectionPromiseRepository extends JpaRepository<CollectionPromise, Long> {

    List<CollectionPromise> findByInvoiceIdAndCompanyCompanyId(Long invoiceId, Long companyId);

    List<CollectionPromise> findByCompanyCompanyIdAndPromisedDateBetweenAndFulfilledFalse(Long companyId, java.time.LocalDate start, java.time.LocalDate end);
}
