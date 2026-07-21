package com.MuhasebePlus.demo.accounting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GeneralLedgerRowDto(
        Long lineId,
        LocalDate entryDate,
        String entryNumber,
        String description,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        BigDecimal runningBalance
) {}
