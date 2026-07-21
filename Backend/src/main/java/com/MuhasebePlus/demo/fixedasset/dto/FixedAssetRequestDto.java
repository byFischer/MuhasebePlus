package com.MuhasebePlus.demo.fixedasset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetRequestDto(
        Long categoryId,
        String name,
        String serialNumber,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        BigDecimal salvageValue
) {}
