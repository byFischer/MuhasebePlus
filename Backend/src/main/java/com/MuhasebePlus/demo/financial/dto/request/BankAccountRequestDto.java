package com.MuhasebePlus.demo.financial.dto.request;

import com.MuhasebePlus.demo.financial.entity.Currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankAccountRequestDto(
        @NotBlank(message = "Bank name is required")
        @Size(max = 100)
        String bankName,

        @NotBlank(message = "IBAN is required")
        @Pattern(regexp = "^TR[0-9]{24}$", message = "IBAN must be a valid Turkish IBAN (TR + 24 digits)")
        String iban,

        @NotNull(message = "Currency is required")
        Currency currency
) {
}
