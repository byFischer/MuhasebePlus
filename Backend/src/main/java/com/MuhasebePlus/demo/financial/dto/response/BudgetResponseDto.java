package com.MuhasebePlus.demo.financial.dto.response;

import com.MuhasebePlus.demo.financial.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponseDto(
    Long budgetId,
    Integer year,
    Integer month,
    String category,
    TransactionType transactionType,
    BigDecimal plannedAmount,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
