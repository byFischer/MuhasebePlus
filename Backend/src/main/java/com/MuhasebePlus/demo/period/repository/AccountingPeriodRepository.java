package com.MuhasebePlus.demo.period.repository;

import com.MuhasebePlus.demo.period.entity.AccountingPeriod;
import com.MuhasebePlus.demo.period.entity.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findByCompanyCompanyIdAndYearAndMonth(Long companyId, int year, int month);

    List<AccountingPeriod> findByCompanyCompanyIdAndYearOrderByMonthAsc(Long companyId, int year);

    List<AccountingPeriod> findByCompanyCompanyIdOrderByYearDescMonthDesc(Long companyId);

    boolean existsByCompanyCompanyIdAndYearAndMonth(Long companyId, int year, int month);

    Optional<AccountingPeriod> findByCompanyCompanyIdAndYearAndMonthAndStatus(
            Long companyId, int year, int month, PeriodStatus status);
}
