package com.MuhasebePlus.demo.financial.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.financial.entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);

    List<Budget> findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthAsc(Long companyId);

    List<Budget> findByCompanyCompanyIdAndYearAndIsDeletedFalse(Long companyId, Integer year);

    Optional<Budget> findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(Long budgetId, Long companyId);

    List<Budget> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
