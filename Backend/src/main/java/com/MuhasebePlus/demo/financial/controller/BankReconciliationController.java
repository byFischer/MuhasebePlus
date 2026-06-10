package com.MuhasebePlus.demo.financial.controller;

import com.MuhasebePlus.demo.financial.dto.request.ManualMatchRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementResponseDto;
import com.MuhasebePlus.demo.financial.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bank-reconciliation")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<BankStatementResponseDto>> getAll() {
        return ResponseEntity.ok(reconciliationService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankStatementResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getById(id));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankStatementResponseDto> importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankAccountId") Long bankAccountId,
            @RequestParam(value = "statementDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate,
            @RequestParam(value = "openingBalance", required = false) BigDecimal openingBalance,
            @RequestParam(value = "closingBalance", required = false) BigDecimal closingBalance) {
        return ResponseEntity.ok(
                reconciliationService.importCsv(file, bankAccountId, statementDate, openingBalance, closingBalance));
    }

    @PostMapping("/{id}/auto-match")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankStatementResponseDto> autoMatch(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.autoMatch(id));
    }

    @PostMapping("/match")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankStatementResponseDto> manualMatch(@RequestBody ManualMatchRequestDto req) {
        return ResponseEntity.ok(reconciliationService.manualMatch(req));
    }

    @DeleteMapping("/match/{lineId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankStatementResponseDto> unmatch(@PathVariable Long lineId) {
        return ResponseEntity.ok(reconciliationService.unmatch(lineId));
    }
}
