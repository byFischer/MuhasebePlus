package com.MuhasebePlus.demo.declaration.dto;

import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;

import java.math.BigDecimal;
import java.util.List;

public record WithholdingDeclarationResponseDto(
        Long declarationId,
        Integer year,
        Integer month,
        DeclarationStatus status,
        BigDecimal totalWithholdingBase,
        BigDecimal totalWithholdingAmount,
        List<WithholdingDeclarationLineDto> lines
) {}
