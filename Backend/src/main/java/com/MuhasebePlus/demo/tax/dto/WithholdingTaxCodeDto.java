package com.MuhasebePlus.demo.tax.dto;

import java.math.BigDecimal;

public record WithholdingTaxCodeDto(
        Integer codeId,
        String code,
        String description,
        BigDecimal withholdingRate,
        Integer denominator,
        Integer numerator,
        String serviceCategory,
        boolean isActive
) {}
