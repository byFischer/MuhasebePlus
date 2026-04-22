package com.MuhasebePlus.demo.invoice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InvoiceLineItemRequestDto(
    @NotNull(message = "Product ID cannot be null")
    Integer productId,

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity
) {
}
