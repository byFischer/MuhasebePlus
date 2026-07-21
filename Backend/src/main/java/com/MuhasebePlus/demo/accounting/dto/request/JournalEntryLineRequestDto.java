package com.MuhasebePlus.demo.accounting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record JournalEntryLineRequestDto(
        @NotNull Long accountId,
        @NotNull @DecimalMin("0.00") BigDecimal debitAmount,
        @NotNull @DecimalMin("0.00") BigDecimal creditAmount,
        @Size(max = 500) String description
) {}
