package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReconciliationItemDto(
    String invoiceNumber,
    LocalDate invoiceDate,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal remainingAmount,
    long daysOverdue
) {
}
