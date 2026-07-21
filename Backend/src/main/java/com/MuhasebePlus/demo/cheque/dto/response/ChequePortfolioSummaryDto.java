package com.MuhasebePlus.demo.cheque.dto.response;

import java.math.BigDecimal;

public record ChequePortfolioSummaryDto(
        long totalCount,
        BigDecimal totalAmount,
        long inPortfolioCount,
        BigDecimal inPortfolioAmount,
        long depositedCount,
        BigDecimal depositedAmount,
        long collectedCount,
        BigDecimal collectedAmount,
        long bouncedCount,
        BigDecimal bouncedAmount,
        long due0To30Count,
        BigDecimal due0To30Amount,
        long due31To60Count,
        BigDecimal due31To60Amount,
        long due60PlusCount,
        BigDecimal due60PlusAmount
) {}
