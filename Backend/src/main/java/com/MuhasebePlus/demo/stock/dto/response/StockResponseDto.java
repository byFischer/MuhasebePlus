package com.MuhasebePlus.demo.stock.dto.response;

import java.time.LocalDateTime;

public record StockResponseDto(
    Integer stockId,
    Integer productId,
    String productName,
    String barcode,
    Integer quantity,
    Integer minQuantity,
    boolean isLowStock,
    LocalDateTime lastCountDate,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
