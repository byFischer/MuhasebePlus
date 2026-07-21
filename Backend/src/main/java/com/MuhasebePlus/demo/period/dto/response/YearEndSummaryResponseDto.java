package com.MuhasebePlus.demo.period.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record YearEndSummaryResponseDto(
    int year,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netProfit,
    Long closingJournalEntryId,
    LocalDateTime closedAt,
    String message
) {}
