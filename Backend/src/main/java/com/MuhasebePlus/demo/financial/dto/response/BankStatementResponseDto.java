package com.MuhasebePlus.demo.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BankStatementResponseDto(
    Long id,
    Long bankAccountId,
    String bankAccountName,
    LocalDate statementDate,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    String status,
    long totalLines,
    long matchedLines,
    List<BankStatementLineDto> lines,
    LocalDateTime createdAt
) {}
