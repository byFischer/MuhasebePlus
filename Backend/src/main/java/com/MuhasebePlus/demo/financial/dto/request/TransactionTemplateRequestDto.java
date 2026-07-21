package com.MuhasebePlus.demo.financial.dto.request;

import java.math.BigDecimal;

import com.MuhasebePlus.demo.financial.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionTemplateRequestDto(
        @NotBlank(message = "Template name is required")
        @Size(max = 255)
        String templateName,

        @NotNull(message = "Default amount is required")
        @DecimalMin(value = "0.01", message = "Default amount must be greater than 0")
        BigDecimal defaultAmount,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @Size(max = 255)
        String description,

        @Size(max = 100)
        String category
) {
}
