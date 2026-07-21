package com.MuhasebePlus.demo.accounting.dto.response;

import com.MuhasebePlus.demo.accounting.entity.AccountType;

import java.math.BigDecimal;

public record FinancialStatementLineDto(
        Long accountId,
        String accountCode,
        String accountName,
        AccountType accountType,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal amount
) {}
