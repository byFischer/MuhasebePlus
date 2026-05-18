package com.MuhasebePlus.demo.period.controller;

import com.MuhasebePlus.demo.period.dto.request.ClosePeriodRequestDto;
import com.MuhasebePlus.demo.period.dto.request.ReopenPeriodRequestDto;
import com.MuhasebePlus.demo.period.dto.response.AccountingPeriodResponseDto;
import com.MuhasebePlus.demo.period.service.AccountingPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounting-periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<AccountingPeriodResponseDto>> listPeriods(
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(accountingPeriodService.listPeriods(targetYear));
    }

    @GetMapping("/{year}/{month}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AccountingPeriodResponseDto> getPeriodStatus(
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(accountingPeriodService.getPeriodStatus(year, month));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AccountingPeriodResponseDto> getCurrentMonthStatus() {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(accountingPeriodService.getPeriodStatus(now.getYear(), now.getMonthValue()));
    }

    @PostMapping("/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodResponseDto> closePeriod(
            @Valid @RequestBody ClosePeriodRequestDto dto) {
        return ResponseEntity.ok(accountingPeriodService.closePeriod(dto));
    }

    @PostMapping("/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodResponseDto> reopenPeriod(
            @Valid @RequestBody ReopenPeriodRequestDto dto) {
        return ResponseEntity.ok(accountingPeriodService.reopenPeriod(dto));
    }
}
