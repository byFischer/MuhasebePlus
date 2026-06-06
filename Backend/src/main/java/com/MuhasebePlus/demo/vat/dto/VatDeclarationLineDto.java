package com.MuhasebePlus.demo.vat.dto;

import java.math.BigDecimal;

public record VatDeclarationLineDto(
    String lineType,
    Integer vatRate,
    BigDecimal taxBase,
    BigDecimal vatAmount
) {}
