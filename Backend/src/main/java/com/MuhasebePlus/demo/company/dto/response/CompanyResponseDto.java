package com.MuhasebePlus.demo.company.dto.response;

import java.time.LocalDateTime;

public record CompanyResponseDto(
        Long companyId,
        String companyName,
        String taxOffice,
        String taxNumber,
        String address,
        String city,
        String phone,
        String email,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String taxOfficeCode,
        String tradeRegistryNo,
        String mersisNo,
        String nace,
        String declarationPeriodType,
        String corporateTaxType
) {
}
