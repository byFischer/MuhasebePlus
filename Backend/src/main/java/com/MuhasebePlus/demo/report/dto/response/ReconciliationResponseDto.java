package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReconciliationResponseDto(
    Long customerId,
    String customerName,
    String taxNumber,
    String companyName,
    String companyTaxNumber,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal openingBalance,
    BigDecimal totalInvoiced,
    BigDecimal totalCollected,
    BigDecimal closingBalance,
    List<ReconciliationItemDto> openItems
) {
}
