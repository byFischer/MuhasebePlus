package com.MuhasebePlus.demo.tax.dto;

import com.MuhasebePlus.demo.tax.entity.VatExemptionCategory;

public record VatExemptionCodeDto(
        Integer codeId,
        String code,
        String description,
        VatExemptionCategory category,
        String kdvBeyannamesiSatiri,
        boolean isActive
) {}
