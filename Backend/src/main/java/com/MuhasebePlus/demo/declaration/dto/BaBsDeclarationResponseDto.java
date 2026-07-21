package com.MuhasebePlus.demo.declaration.dto;

import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;

import java.math.BigDecimal;
import java.util.List;

public record BaBsDeclarationResponseDto(
        Long declarationId,
        String babsType,
        Integer year,
        Integer month,
        DeclarationStatus status,
        Integer recordCount,
        BigDecimal totalAmount,
        List<BaBsDeclarationLineDto> lines
) {}
