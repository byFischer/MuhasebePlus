package com.MuhasebePlus.demo.invoice.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NewProductRequestDto(
    @NotBlank(message = "Barcode cannot be blank")
    @Size(max = 100)
    String barcode,

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100)
    String name,

    @Size(max = 200)
    String description,

    @NotBlank(message = "Unit cannot be blank")
    @Size(max = 50)
    String unit,

    @NotNull(message = "Sale price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal salePrice,

    @NotNull(message = "VAT rate cannot be null")
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal vatRate,

    @NotNull(message = "Cost price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal costPrice,

    @Min(value = 0, message = "Min quantity cannot be negative")
    Integer minQuantity
) {
}
