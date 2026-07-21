package com.MuhasebePlus.demo.financial.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.financial.dto.request.BudgetRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BudgetResponseDto;
import com.MuhasebePlus.demo.financial.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<BudgetResponseDto>> getAll(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(budgetService.getAllBudgets(year));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BudgetResponseDto> create(@RequestBody BudgetRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(dto));
    }

    @PutMapping("/{budgetId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BudgetResponseDto> update(
            @PathVariable Long budgetId,
            @RequestBody BudgetRequestDto dto) {
        return ResponseEntity.ok(budgetService.updateBudget(budgetId, dto));
    }

    @DeleteMapping("/{budgetId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> delete(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }
}
