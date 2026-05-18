package com.MuhasebePlus.demo.cheque.controller;

import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequePortfolioSummaryDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeResponseDto;
import com.MuhasebePlus.demo.cheque.service.ChequeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cheques")
@RequiredArgsConstructor
public class ChequeController {

    private final ChequeService chequeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> createCheque(@Valid @RequestBody ChequeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chequeService.createCheque(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ChequeResponseDto>> getAllCheques(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(chequeService.getChequesByStatus(status));
        }
        return ResponseEntity.ok(chequeService.getAllCheques());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> getChequeById(@PathVariable Long id) {
        return ResponseEntity.ok(chequeService.getChequeById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCheque(@PathVariable Long id) {
        chequeService.deleteCheque(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> deposit(
            @PathVariable Long id,
            @RequestParam Long bankAccountId) {
        return ResponseEntity.ok(chequeService.deposit(id, bankAccountId));
    }

    @PostMapping("/{id}/collect")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> collect(
            @PathVariable Long id,
            @Valid @RequestBody CollectChequeRequestDto dto) {
        return ResponseEntity.ok(chequeService.markAsCollected(id, dto));
    }

    @PostMapping("/{id}/bounce")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> bounce(
            @PathVariable Long id,
            @Valid @RequestBody BounceReasonRequestDto dto) {
        return ResponseEntity.ok(chequeService.markAsBounced(id, dto));
    }

    @PostMapping("/{id}/endorse")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequeResponseDto> endorse(
            @PathVariable Long id,
            @Valid @RequestBody EndorseChequeRequestDto dto) {
        return ResponseEntity.ok(chequeService.endorse(id, dto));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChequeResponseDto> cancel(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ResponseEntity.ok(chequeService.cancel(id, reason));
    }

    @GetMapping("/portfolio/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ChequePortfolioSummaryDto> getPortfolioSummary() {
        return ResponseEntity.ok(chequeService.getPortfolioSummary());
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<ChequeResponseDto>> getUpcoming(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(chequeService.getUpcomingCheques(days));
    }
}
