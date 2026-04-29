package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.AiInsightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInsightLogRepository extends JpaRepository<AiInsightLog, Long> {

    @Query("SELECT COALESCE(SUM(l.inputTokens + l.outputTokens), 0) FROM AiInsightLog l " +
           "WHERE l.company.companyId = :companyId " +
           "AND MONTH(l.createdAt) = MONTH(CURRENT_TIMESTAMP) " +
           "AND YEAR(l.createdAt) = YEAR(CURRENT_TIMESTAMP)")
    int sumTokensThisMonthByCompany(Long companyId);
}
