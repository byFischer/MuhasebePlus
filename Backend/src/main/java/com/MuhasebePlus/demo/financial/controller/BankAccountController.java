package com.MuhasebePlus.demo.financial.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.financial.dto.request.BankAccountRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankAccountResponseDto;
import com.MuhasebePlus.demo.financial.service.BankAccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    @Autowired
    private BankAccountService bankAccountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankAccountResponseDto> createBankAccount(@Valid @RequestBody BankAccountRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createBankAccount(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<BankAccountResponseDto>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankAccountResponseDto> getBankAccountById(@PathVariable Long accountId) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(accountId));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BankAccountResponseDto> updateBankAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody BankAccountRequestDto dto) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(accountId, dto));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long accountId) {
        bankAccountService.softDeleteBankAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{accountId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BankAccountResponseDto> restoreBankAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(bankAccountService.restoreBankAccount(accountId));
    }
}
