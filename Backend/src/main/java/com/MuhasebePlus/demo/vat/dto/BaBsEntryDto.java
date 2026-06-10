package com.MuhasebePlus.demo.vat.dto;

import java.math.BigDecimal;

public record BaBsEntryDto(
    Long customerId,
    String customerName,
    String taxNumber,
    BigDecimal totalAmount,
    BigDecimal totalVat,
    String formType // "BA" or "BS"
) {}
