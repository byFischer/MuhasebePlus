package com.MuhasebePlus.demo.report.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.report.dto.response.ReconciliationResponseDto;
import com.MuhasebePlus.demo.report.dto.response.StatementResponseDto;
import com.MuhasebePlus.demo.report.service.ReconciliationService;
import com.MuhasebePlus.demo.report.service.StatementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;
    private final ReconciliationService reconciliationService;

    @GetMapping("/statement/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<StatementResponseDto> getStatement(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statementService.generateStatement(customerId, startDate, endDate));
    }

    @GetMapping("/reconciliation/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ReconciliationResponseDto> getReconciliation(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(reconciliationService.generateReconciliation(customerId, periodStart, periodEnd));
    }
}
