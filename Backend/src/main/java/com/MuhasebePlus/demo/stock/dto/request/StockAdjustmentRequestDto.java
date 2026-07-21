package com.MuhasebePlus.demo.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequestDto(
    @NotNull
    @Min(1)
    Integer amount,

    @Size(max = 255)
    String reason
) {
}
