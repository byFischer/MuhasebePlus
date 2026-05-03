package com.MuhasebePlus.demo.financial.dto.request;

import com.MuhasebePlus.demo.financial.entity.TransactionType;
import java.math.BigDecimal;

public record BudgetRequestDto(
    Integer year,
    Integer month,
    String category,
    TransactionType transactionType,
    BigDecimal plannedAmount,
    String notes
) {}
