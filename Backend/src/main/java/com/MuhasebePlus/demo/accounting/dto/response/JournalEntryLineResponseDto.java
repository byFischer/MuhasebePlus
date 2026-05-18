package com.MuhasebePlus.demo.accounting.dto.response;

import java.math.BigDecimal;

public record JournalEntryLineResponseDto(
        Long lineId,
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String description,
        int lineOrder
) {}
