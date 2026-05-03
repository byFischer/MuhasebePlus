package com.MuhasebePlus.demo.financial.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.BudgetRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BudgetResponseDto;
import com.MuhasebePlus.demo.financial.entity.Budget;
import com.MuhasebePlus.demo.financial.repository.BudgetRepository;

@Service
@Transactional
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;

    public List<BudgetResponseDto> getAllBudgets(Integer year) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Budget> budgets = year != null
                ? budgetRepository.findByCompanyCompanyIdAndYearAndIsDeletedFalse(companyId, year)
                : budgetRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthAsc(companyId);
        return budgets.stream().map(this::toDto).toList();
    }

    public BudgetResponseDto createBudget(BudgetRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Budget budget = new Budget();
        budget.setCompany(companyRepository.getReferenceById(companyId));
        budget.setYear(dto.year());
        budget.setMonth(dto.month());
        budget.setCategory(dto.category());
        budget.setTransactionType(dto.transactionType());
        budget.setPlannedAmount(dto.plannedAmount());
        budget.setNotes(dto.notes());
        budget.setDeleted(false);
        return toDto(budgetRepository.save(budget));
    }

    public BudgetResponseDto updateBudget(Long budgetId, BudgetRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Budget budget = budgetRepository
                .findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(budgetId, companyId)
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı: " + budgetId));
        budget.setYear(dto.year());
        budget.setMonth(dto.month());
        budget.setCategory(dto.category());
        budget.setTransactionType(dto.transactionType());
        budget.setPlannedAmount(dto.plannedAmount());
        budget.setNotes(dto.notes());
        return toDto(budgetRepository.save(budget));
    }

    public void deleteBudget(Long budgetId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Budget budget = budgetRepository
                .findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(budgetId, companyId)
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı: " + budgetId));
        budget.setDeleted(true);
        budget.setDeletedAt(LocalDateTime.now());
        budgetRepository.save(budget);
    }

    private BudgetResponseDto toDto(Budget b) {
        return new BudgetResponseDto(
                b.getBudgetId(),
                b.getYear(),
                b.getMonth(),
                b.getCategory(),
                b.getTransactionType(),
                b.getPlannedAmount(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
