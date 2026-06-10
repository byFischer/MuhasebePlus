package com.MuhasebePlus.demo.declaration.dto;

import java.math.BigDecimal;

public record BaBsDeclarationLineDto(
        Long lineId,
        String taxNumber,
        String customerName,
        BigDecimal totalAmount,
        Integer documentCount
) {}
