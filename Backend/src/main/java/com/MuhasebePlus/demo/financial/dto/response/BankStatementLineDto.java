package com.MuhasebePlus.demo.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BankStatementLineDto(
    Long id,
    LocalDate transactionDate,
    String description,
    BigDecimal amount,
    BigDecimal balance,
    boolean isMatched,
    Long matchedTransactionId
) {}
