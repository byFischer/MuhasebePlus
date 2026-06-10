package com.MuhasebePlus.demo.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponseDto(
        Long accountId,
        String bankName,
        String iban,
        String accountType,
        String accountCode,
        String currency,
        BigDecimal currentBalance,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
