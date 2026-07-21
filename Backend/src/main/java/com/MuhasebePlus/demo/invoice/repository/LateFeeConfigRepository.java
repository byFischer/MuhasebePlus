package com.MuhasebePlus.demo.invoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.invoice.entity.LateFeeConfig;

public interface LateFeeConfigRepository extends JpaRepository<LateFeeConfig, Long> {

    Optional<LateFeeConfig> findByCompanyCompanyIdAndIsActiveTrue(Long companyId);
}
