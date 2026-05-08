package com.MuhasebePlus.demo.invoice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InvoiceLineItemRequestDto(
    Integer productId,

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @Valid
    NewProductRequestDto newProduct
) {
    @AssertTrue(message = "productId or newProduct must be provided, but not both")
    public boolean isValidProductReference() {
        return (productId != null) ^ (newProduct != null);
    }
}
