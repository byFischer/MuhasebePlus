package com.MuhasebePlus.demo.tax.repository;

import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithholdingTaxCodeRepository extends JpaRepository<WithholdingTaxCode, Integer> {
    List<WithholdingTaxCode> findByIsActiveTrueOrderByCodeAsc();
}
