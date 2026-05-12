package com.MuhasebePlus.demo.customer.dto.response;

import java.math.BigDecimal;

public record CustomerAgingDto(
    Long customerId,
    String customerName,
    String accountCode,
    BigDecimal totalAR,
    BigDecimal bucket0to30,
    BigDecimal bucket31to60,
    BigDecimal bucket61to90,
    BigDecimal bucket90plus
) {
}
