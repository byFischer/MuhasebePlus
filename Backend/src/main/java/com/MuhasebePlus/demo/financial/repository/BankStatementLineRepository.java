package com.MuhasebePlus.demo.financial.repository;

import com.MuhasebePlus.demo.financial.entity.BankStatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {
    List<BankStatementLine> findByBankStatementId(Long statementId);
    List<BankStatementLine> findByBankStatementIdAndIsMatchedFalse(Long statementId);
    long countByBankStatementIdAndIsMatchedTrue(Long statementId);
    long countByBankStatementId(Long statementId);
}
