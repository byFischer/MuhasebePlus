package com.MuhasebePlus.demo.declaration.dto;

import java.math.BigDecimal;

public record GenerateDeclarationRequestDto(
        Integer year,
        Integer month,
        BigDecimal previousPeriodCarryover
) {}
