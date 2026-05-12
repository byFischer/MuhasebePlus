package com.MuhasebePlus.demo.invoice.controller;

import com.MuhasebePlus.demo.invoice.service.LateFeeService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class LateFeeController {

    private final LateFeeService lateFeeService;
    private final CompanyContext companyContext;

    @GetMapping("/{invoiceId}/late-fee")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getLateFee(@PathVariable Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BigDecimal fee = lateFeeService.calculateLateFee(invoiceId, companyId);
        return ResponseEntity.ok(Map.of("invoiceId", invoiceId, "lateFee", fee));
    }

    @GetMapping("/late-fee-config")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> getLateFeeConfig() {
        Long companyId = companyContext.getCurrentCompanyId();
        return ResponseEntity.ok(lateFeeService.getConfig(companyId));
    }

    @PutMapping("/late-fee-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateLateFeeConfig(@RequestParam BigDecimal monthlyRate, @RequestParam Integer gracePeriodDays) {
        Long companyId = companyContext.getCurrentCompanyId();
        return ResponseEntity.ok(lateFeeService.saveConfig(companyId, monthlyRate, gracePeriodDays));
    }
}
