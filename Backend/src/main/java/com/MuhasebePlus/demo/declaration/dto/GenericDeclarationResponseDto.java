package com.MuhasebePlus.demo.declaration.dto;

import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;

import java.math.BigDecimal;
import java.util.Map;

public record GenericDeclarationResponseDto(
        Long declarationId,
        String declarationType,
        Integer year,
        Integer period,
        DeclarationStatus status,
        BigDecimal totalPayable,
        Map<String, Object> dataSnapshot
) {}
