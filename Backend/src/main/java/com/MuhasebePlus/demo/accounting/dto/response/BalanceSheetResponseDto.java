package com.MuhasebePlus.demo.accounting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceSheetResponseDto(
        LocalDate asOfDate,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquity,
        BigDecimal totalLiabilitiesAndEquity,
        BigDecimal difference,
        List<FinancialStatementSectionDto> assetSections,
        List<FinancialStatementSectionDto> liabilitySections,
        List<FinancialStatementSectionDto> equitySections
) {}
