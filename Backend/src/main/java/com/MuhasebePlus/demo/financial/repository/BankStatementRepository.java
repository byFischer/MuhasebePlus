package com.MuhasebePlus.demo.financial.repository;

import com.MuhasebePlus.demo.financial.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByCompanyCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<BankStatement> findByIdAndCompanyCompanyId(Long id, Long companyId);
}
