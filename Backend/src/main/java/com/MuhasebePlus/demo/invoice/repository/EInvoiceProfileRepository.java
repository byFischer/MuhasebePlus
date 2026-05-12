package com.MuhasebePlus.demo.invoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;

public interface EInvoiceProfileRepository extends JpaRepository<EInvoiceProfile, Long> {

    Optional<EInvoiceProfile> findByCompanyCompanyIdAndIsActiveTrue(Long companyId);
}
