package com.MuhasebePlus.demo.vat.repository;

import com.MuhasebePlus.demo.vat.entity.VatDeclarationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VatDeclarationLineRepository extends JpaRepository<VatDeclarationLine, Long> {
    List<VatDeclarationLine> findByVatPeriodId(Long vatPeriodId);
    void deleteByVatPeriodId(Long vatPeriodId);
}
