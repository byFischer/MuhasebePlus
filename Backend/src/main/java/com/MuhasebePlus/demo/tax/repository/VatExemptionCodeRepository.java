package com.MuhasebePlus.demo.tax.repository;

import com.MuhasebePlus.demo.tax.entity.VatExemptionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VatExemptionCodeRepository extends JpaRepository<VatExemptionCode, Integer> {
    List<VatExemptionCode> findByIsActiveTrueOrderByCodeAsc();
}
