package com.MuhasebePlus.demo.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequestDto(
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        String taxOffice,

        @Size(max = 20)
        String taxNumber,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String email,

        @Size(max = 10)
        String taxOfficeCode,

        @Size(max = 30)
        String tradeRegistryNo,

        @Size(max = 20)
        String mersisNo,

        @Size(max = 10)
        String nace,

        @Size(max = 20)
        String declarationPeriodType,

        @Size(max = 30)
        String corporateTaxType
) {
}
