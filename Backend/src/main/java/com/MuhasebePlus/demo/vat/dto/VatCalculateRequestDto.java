package com.MuhasebePlus.demo.vat.dto;

public record VatCalculateRequestDto(
    Integer year,
    Integer month,
    // Önceki dönemden devir eden KDV (opsiyonel)
    java.math.BigDecimal previousCarriedForward
) {}
