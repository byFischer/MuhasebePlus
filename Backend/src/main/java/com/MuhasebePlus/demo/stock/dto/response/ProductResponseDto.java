package com.MuhasebePlus.demo.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDto(
    Integer productId,
    String barcode,
    String name,
    String description,
    String unit,
    BigDecimal salePrice,
    BigDecimal vatRate,
    BigDecimal costPrice,
    Integer quantity,
    Integer minQuantity,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
