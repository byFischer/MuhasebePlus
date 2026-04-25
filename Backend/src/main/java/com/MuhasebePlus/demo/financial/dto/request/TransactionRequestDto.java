package com.MuhasebePlus.demo.financial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.MuhasebePlus.demo.financial.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record TransactionRequestDto(
        @NotNull(message = "Account id is required")
        Long accountId,

        Long invoiceId,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Transaction date is required")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        LocalDate transactionDate,

        @Size(max = 255)
        String description,

        @Size(max = 100)
        String category,

        Boolean isRecurring
) {
}
