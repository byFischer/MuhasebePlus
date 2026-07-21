package com.MuhasebePlus.demo.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.MuhasebePlus.demo.stock.entity.MovementType;

public record StockMovementResponseDto(
    Long movementId,
    Integer productId,
    String productName,
    Integer quantity,
    MovementType movementType,
    String sourceType,
    Long sourceId,
    BigDecimal unitCost,
    String reason,
    LocalDateTime createdAt
) {
}
