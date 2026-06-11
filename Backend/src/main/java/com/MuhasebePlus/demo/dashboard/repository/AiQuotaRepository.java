package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiQuotaRepository extends JpaRepository<AiQuota, Long> {

    Optional<AiQuota> findByCompanyCompanyId(Long companyId);

    List<AiQuota> findTop10ByOrderByTokensUsedThisMonthDesc();
}
