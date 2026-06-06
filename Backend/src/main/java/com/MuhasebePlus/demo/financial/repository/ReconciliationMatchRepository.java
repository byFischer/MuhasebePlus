package com.MuhasebePlus.demo.financial.repository;

import com.MuhasebePlus.demo.financial.entity.ReconciliationMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationMatchRepository extends JpaRepository<ReconciliationMatch, Long> {
    List<ReconciliationMatch> findByStatementLineId(Long lineId);
    Optional<ReconciliationMatch> findByStatementLineIdAndCompanyId(Long lineId, Long companyId);
    void deleteByStatementLineId(Long lineId);
}
