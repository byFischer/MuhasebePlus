package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.BudgetRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BudgetResponseDto;
import com.MuhasebePlus.demo.financial.entity.Budget;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceCoverageTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CompanyContext companyContext;
    @Mock
    private CompanyRepository companyRepository;

    private BudgetService service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetRepository, companyContext, companyRepository);
        company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
    }

    @Test
    void getAllBudgets_usesYearFilterWhenPresentAndDefaultListOtherwise() {
        Budget budget = budget(10L, 2026, 5, "Kira", "1000.00");
        when(budgetRepository.findByCompanyCompanyIdAndYearAndIsDeletedFalse(1L, 2026))
                .thenReturn(List.of(budget));
        when(budgetRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthAsc(1L))
                .thenReturn(List.of(budget));

        assertThat(service.getAllBudgets(2026)).singleElement().satisfies(dto -> {
            assertThat(dto.budgetId()).isEqualTo(10L);
            assertThat(dto.plannedAmount()).isEqualByComparingTo("1000.00");
        });
        assertThat(service.getAllBudgets(null)).hasSize(1);
    }

    @Test
    void createBudget_mapsRequestAndCompanyReference() {
        BudgetRequestDto request = request("Gelir", "1500.00");
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(budgetRepository.save(org.mockito.ArgumentMatchers.any(Budget.class)))
                .thenAnswer(invocation -> {
                    Budget budget = invocation.getArgument(0);
                    budget.setBudgetId(55L);
                    return budget;
                });

        BudgetResponseDto result = service.createBudget(request);

        assertThat(result.budgetId()).isEqualTo(55L);
        assertThat(result.category()).isEqualTo("Gelir");
        assertThat(result.transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void updateBudget_whenFoundUpdatesFields_whenMissingThrows() {
        Budget budget = budget(22L, 2025, 1, "Eski", "100.00");
        when(budgetRepository.findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(22L, 1L))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(budget)).thenReturn(budget);

        BudgetResponseDto result = service.updateBudget(22L, request("Yeni", "750.00"));

        assertThat(result.category()).isEqualTo("Yeni");
        assertThat(result.plannedAmount()).isEqualByComparingTo("750.00");
        assertThatThrownBy(() -> service.updateBudget(99L, request("Yok", "1.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("B");
    }

    @Test
    void deleteBudget_softDeletesDraftBudgetAndThrowsWhenMissing() {
        Budget budget = budget(33L, 2026, 6, "Kira", "100.00");
        when(budgetRepository.findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(33L, 1L))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.findByBudgetIdAndCompanyCompanyIdAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        service.deleteBudget(33L);

        assertThat(budget.isDeleted()).isTrue();
        assertThat(budget.getDeletedAt()).isNotNull();
        verify(budgetRepository).save(budget);
        assertThatThrownBy(() -> service.deleteBudget(99L))
                .isInstanceOf(RuntimeException.class);
    }

    private BudgetRequestDto request(String category, String plannedAmount) {
        return new BudgetRequestDto(
                2026,
                5,
                category,
                TransactionType.INCOME,
                new BigDecimal(plannedAmount),
                "Not"
        );
    }

    private Budget budget(Long id, Integer year, Integer month, String category, String plannedAmount) {
        Budget budget = new Budget();
        budget.setBudgetId(id);
        budget.setCompany(company);
        budget.setYear(year);
        budget.setMonth(month);
        budget.setCategory(category);
        budget.setTransactionType(TransactionType.INCOME);
        budget.setPlannedAmount(new BigDecimal(plannedAmount));
        budget.setNotes("Not");
        budget.setDeleted(false);
        org.springframework.test.util.ReflectionTestUtils.setField(budget, "createdAt", LocalDateTime.of(2026, 1, 1, 10, 0));
        org.springframework.test.util.ReflectionTestUtils.setField(budget, "updatedAt", LocalDateTime.of(2026, 1, 2, 10, 0));
        return budget;
    }
}
