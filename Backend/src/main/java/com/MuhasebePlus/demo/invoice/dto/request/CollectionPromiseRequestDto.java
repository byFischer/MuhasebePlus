package com.MuhasebePlus.demo.invoice.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectionPromiseRequestDto(
    @NotNull(message = "Promised date is required")
    @FutureOrPresent(message = "Promised date must be today or in the future")
    LocalDate promisedDate,

    @NotNull(message = "Promised amount is required")
    @Positive(message = "Promised amount must be positive")
    BigDecimal promisedAmount,

    @Size(max = 500)
    String notes
) {
}
