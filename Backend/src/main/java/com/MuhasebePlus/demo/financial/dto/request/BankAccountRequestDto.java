package com.MuhasebePlus.demo.financial.dto.request;

import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BankAccountRequestDto(
        @NotBlank(message = "Bank name is required")
        @Size(max = 100)
        String bankName,

        @Size(max = 50)
        String iban,

        BankAccountType accountType,

        @NotNull(message = "Currency is required")
        Currency currency
) {
}
