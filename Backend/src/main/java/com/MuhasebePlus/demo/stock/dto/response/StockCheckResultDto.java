package com.MuhasebePlus.demo.stock.dto.response;

public record StockCheckResultDto(
    Integer productId,
    Integer requestedQuantity,
    Integer availableQuantity,
    boolean isSufficient
) {
    
}
