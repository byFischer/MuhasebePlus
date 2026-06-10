package com.MuhasebePlus.demo.fixedasset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetResponseDto(
        Long id,
        String name,
        String serialNumber,
        Long categoryId,
        String categoryName,
        String depreciationMethod,
        Integer usefulLifeMonths,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        BigDecimal salvageValue,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        String status,
        LocalDate disposedAt
) {}
