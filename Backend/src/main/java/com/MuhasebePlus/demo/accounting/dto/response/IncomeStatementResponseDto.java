package com.MuhasebePlus.demo.accounting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeStatementResponseDto(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        BigDecimal profitMarginPercent,
        List<FinancialStatementSectionDto> sections
) {}
