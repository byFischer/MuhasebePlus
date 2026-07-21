package com.MuhasebePlus.demo.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CollectionPromiseResponseDto(
    Long promiseId,
    Long invoiceId,
    LocalDate promisedDate,
    BigDecimal promisedAmount,
    String notes,
    boolean fulfilled,
    LocalDate fulfilledAt,
    LocalDateTime createdAt
) {
}
