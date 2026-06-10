package com.MuhasebePlus.demo.declaration.dto;

import java.math.BigDecimal;

public record VatDeclarationLineDto(
        Long lineId,
        String lineCode,
        String description,
        BigDecimal baseAmount,
        BigDecimal vatAmount,
        Integer lineOrder
) {}
