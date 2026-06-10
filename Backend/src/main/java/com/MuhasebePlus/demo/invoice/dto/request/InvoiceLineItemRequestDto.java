package com.MuhasebePlus.demo.invoice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceLineItemRequestDto(
    Integer productId,

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @Valid
    NewProductRequestDto newProduct,

    @DecimalMin(value = "0", message = "Discount rate cannot be negative")
    @DecimalMax(value = "100", message = "Discount rate cannot exceed 100")
    BigDecimal discountRate,

    @DecimalMin(value = "0", message = "Withholding tax rate cannot be negative")
    @DecimalMax(value = "100", message = "Withholding tax rate cannot exceed 100")
    BigDecimal withholdingTaxRate,

    Integer withholdingTaxCodeId,

    Integer vatExemptionCodeId,

    String vatExemptionReason
) {
    @AssertTrue(message = "productId or newProduct must be provided, but not both")
    public boolean isValidProductReference() {
        return (productId != null) ^ (newProduct != null);
    }
}
