package com.MuhasebePlus.demo.accounting.controller;

import com.MuhasebePlus.demo.accounting.dto.request.ChartOfAccountRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.ChartOfAccountResponseDto;
import com.MuhasebePlus.demo.accounting.service.ChartOfAccountService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;
    private final CompanyContext companyContext;

    @PostMapping("/seed-tdhp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> seedTdhp() {
        chartOfAccountService.copyTdhpForCompany(companyContext.getCurrentCompanyId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ChartOfAccountResponseDto>> getAll() {
        return ResponseEntity.ok(chartOfAccountService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChartOfAccountResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chartOfAccountService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChartOfAccountResponseDto> create(@Valid @RequestBody ChartOfAccountRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartOfAccountService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChartOfAccountResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ChartOfAccountRequestDto dto) {
        return ResponseEntity.ok(chartOfAccountService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chartOfAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
