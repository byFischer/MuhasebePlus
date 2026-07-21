package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementResponseDto(
    Long customerId,
    String customerName,
    String taxNumber,
    String accountCode,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    List<StatementLineDto> lines
) {
}
