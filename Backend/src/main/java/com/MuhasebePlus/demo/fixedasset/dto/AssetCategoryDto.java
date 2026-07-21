package com.MuhasebePlus.demo.fixedasset.dto;

import java.math.BigDecimal;

public record AssetCategoryDto(
        Long id,
        String name,
        String depreciationMethod,
        Integer usefulLifeMonths,
        BigDecimal annualRate,
        String assetAccountCode,
        String depreciationAccountCode
) {}
