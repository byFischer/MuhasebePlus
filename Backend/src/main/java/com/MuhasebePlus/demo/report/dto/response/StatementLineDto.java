package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatementLineDto(
    LocalDate date,
    String description,
    String documentNumber,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal balance
) {
}
