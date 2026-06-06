package com.MuhasebePlus.demo.customer.dto.request;

import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.financial.entity.Currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 180)
        String name,

        @NotBlank(message = "Tax number is required")
        @Pattern(regexp = "\\d{10,11}", message = "Tax number must be 10 or 11 digits")
        String taxNumber,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String city,

        @Pattern(regexp = "^(\\+90)?[1-9][0-9]{9}$", message = "Phone number must be valid Turkish format")
        String phoneNumber,

        @NotNull(message = "Type is required")
        CustomerType type,

        @jakarta.validation.constraints.Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String accountCode,

        BigDecimal openingBalance,

        LocalDate openingBalanceDate,

        @Size(max = 100)
        String taxOffice,

        @Pattern(regexp = "\\d{11}", message = "Identity number must be 11 digits")
        String identityNumber,

        @Size(max = 34)
        String iban,

        Currency currency,

        BigDecimal creditLimit,

        @NotNull(message = "Customer role is required")
        CustomerRole customerRole,

        CustomerStatus status,

        @Size(max = 50)
        String customerGroup
) {
}
