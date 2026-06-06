package com.MuhasebePlus.demo.accounting.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FinancialStatementSectionDto(
        String sectionCode,
        String sectionName,
        BigDecimal totalAmount,
        List<FinancialStatementLineDto> lines
) {}
