package com.MuhasebePlus.demo.vat.repository;

import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VatPeriodRepository extends JpaRepository<VatPeriod, Long> {
    List<VatPeriod> findByCompanyCompanyIdOrderByYearDescMonthDesc(Long companyId);
    Optional<VatPeriod> findByCompanyCompanyIdAndYearAndMonth(Long companyId, Integer year, Integer month);
    Optional<VatPeriod> findByIdAndCompanyCompanyId(Long id, Long companyId);
}
