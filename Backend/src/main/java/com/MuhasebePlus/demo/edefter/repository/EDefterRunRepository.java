package com.MuhasebePlus.demo.edefter.repository;

import com.MuhasebePlus.demo.edefter.entity.EDefterRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EDefterRunRepository extends JpaRepository<EDefterRun, Long> {

    List<EDefterRun> findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(Long companyId);

    List<EDefterRun> findByCompanyCompanyIdAndYearAndIsDeletedFalseOrderByMonthAsc(Long companyId, int year);

    Optional<EDefterRun> findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(Long companyId, int year, int month);

    Optional<EDefterRun> findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(Long runId, Long companyId);
}
