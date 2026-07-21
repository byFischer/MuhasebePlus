package com.MuhasebePlus.demo.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionTemplateResponseDto(
        Long templateId,
        String templateName,
        BigDecimal defaultAmount,
        String transactionType,
        String description,
        String category,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
