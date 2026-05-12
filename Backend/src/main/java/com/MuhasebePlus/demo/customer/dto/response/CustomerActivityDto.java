package com.MuhasebePlus.demo.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerActivityDto(
    LocalDate date,
    String type,
    String description,
    String documentNo,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal balance
) {
}
