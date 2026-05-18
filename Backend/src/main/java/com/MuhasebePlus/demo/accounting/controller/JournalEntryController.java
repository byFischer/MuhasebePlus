package com.MuhasebePlus.demo.accounting.controller;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.GeneralLedgerRowDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<JournalEntryResponseDto>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<JournalEntryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(journalEntryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JournalEntryResponseDto> createManual(
            @Valid @RequestBody JournalEntryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryService.createManualEntry(dto));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JournalEntryResponseDto> reverse(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(journalEntryService.reverseEntry(id, reason));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        journalEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TrialBalanceRowDto>> trialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(journalEntryService.getTrialBalance(startDate, endDate));
    }

    @GetMapping("/general-ledger/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<GeneralLedgerRowDto>> generalLedger(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(journalEntryService.getGeneralLedger(accountId, startDate, endDate));
    }
}
