package com.MuhasebePlus.demo.declaration.dto;

import java.math.BigDecimal;

public record WithholdingDeclarationLineDto(
        Long lineId,
        Integer withholdingTaxCodeId,
        String withholdingCode,
        String description,
        BigDecimal baseAmount,
        BigDecimal withholdingAmount,
        Integer documentCount,
        Integer lineOrder
) {}
