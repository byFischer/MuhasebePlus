package com.MuhasebePlus.demo.fixedasset.dto;

import java.math.BigDecimal;

public record DepreciationLineDto(
        Long id,
        Integer periodYear,
        Integer periodMonth,
        BigDecimal depreciationAmount,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        Long journalEntryId
) {}
