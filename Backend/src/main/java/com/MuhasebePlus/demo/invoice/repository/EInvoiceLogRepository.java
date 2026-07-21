package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.EInvoiceLog;

public interface EInvoiceLogRepository extends JpaRepository<EInvoiceLog, Long> {

    List<EInvoiceLog> findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(Long invoiceId, Long companyId);

    List<EInvoiceLog> findByCompanyCompanyIdOrderByCreatedAtDesc(Long companyId);
}
