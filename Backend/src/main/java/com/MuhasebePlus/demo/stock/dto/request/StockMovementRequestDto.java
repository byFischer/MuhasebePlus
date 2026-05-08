package com.MuhasebePlus.demo.stock.dto.request;

import java.math.BigDecimal;

import com.MuhasebePlus.demo.stock.entity.MovementType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockMovementRequestDto(
    @NotNull(message = "Product ID cannot be null")
    Integer productId,

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @NotNull(message = "Movement type cannot be null")
    MovementType movementType,

    @Size(max = 255, message = "Reason must be at most 255 characters")
    String reason,

    BigDecimal unitCost
) {
}
