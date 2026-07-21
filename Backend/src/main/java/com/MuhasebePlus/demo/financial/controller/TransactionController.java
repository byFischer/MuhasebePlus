package com.MuhasebePlus.demo.financial.controller;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionResponseDto;
import com.MuhasebePlus.demo.financial.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionResponseDto> createTransaction(@Valid @RequestBody TransactionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<TransactionResponseDto>> getAllTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (accountId != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
        }
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(transactionService.getTransactionsByType(type));
        }
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByDateRange(startDate, endDate));
        }
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionRequestDto dto) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, dto));
    }

    @DeleteMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {
        transactionService.softDeleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{transactionId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> restoreTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.restoreTransaction(transactionId));
    }
}
