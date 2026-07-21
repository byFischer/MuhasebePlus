package com.MuhasebePlus.demo.declaration.dto;

import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VatDeclarationResponseDto(
        Long declarationId,
        String declarationType,
        Integer year,
        Integer month,
        DeclarationStatus status,
        BigDecimal totalSalesBase,
        BigDecimal totalSalesVat,
        BigDecimal totalPurchasesBase,
        BigDecimal totalPurchasesVat,
        BigDecimal previousPeriodCarryover,
        BigDecimal payableVat,
        BigDecimal nextPeriodCarryover,
        LocalDateTime generatedAt,
        LocalDateTime submittedAt,
        List<VatDeclarationLineDto> lines
) {}
