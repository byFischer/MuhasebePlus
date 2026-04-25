package com.MuhasebePlus.demo.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        Long transactionId,
        Long accountId,
        Long invoiceId,
        String transactionType,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        String category,
        boolean isRecurring,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
