package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;

public interface InvoiceSeriesRepository extends JpaRepository<InvoiceSeries, Long> {

    Optional<InvoiceSeries> findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(Long companyId, String seriesCode, InvoiceType invoiceType);

    List<InvoiceSeries> findByCompanyCompanyIdAndIsActiveTrue(Long companyId);

    boolean existsByCompanyCompanyIdAndSeriesCodeAndInvoiceType(Long companyId, String seriesCode, InvoiceType invoiceType);
}
