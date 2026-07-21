package com.MuhasebePlus.demo.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockCheckItemDto(

    @NotNull
    Integer productId,

    @NotNull
    @Min(0)
    Integer quantity
) {
    
}
