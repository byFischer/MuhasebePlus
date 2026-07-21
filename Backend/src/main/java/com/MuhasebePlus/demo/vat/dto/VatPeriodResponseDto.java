package com.MuhasebePlus.demo.vat.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VatPeriodResponseDto(
    Long id,
    Integer year,
    Integer month,
    String status,
    BigDecimal totalOutputVat,
    BigDecimal totalInputVat,
    BigDecimal netPayableVat,
    BigDecimal previousCarriedForwardVat,
    BigDecimal carriedForwardVat,
    Long settlementJournalEntryId,
    LocalDateTime lockedAt,
    Long lockedBy,
    List<VatDeclarationLineDto> lines,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
