package com.MuhasebePlus.demo.stock.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequestDto(

        @NotBlank(message = "Barcode cannot be blank")
        @Size(max = 100)
        String barcode,

        @NotBlank(message = "name cannot be blank")
        @Size(max = 100)
        String name,

        @Size(max = 200)
        String description,

        @NotBlank(message = "unit cannot be blank")
        @Size(max = 50)
        String unit,

        @NotNull(message = "salePrice cannot be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "salePrice must be greater than zero")
        BigDecimal salePrice,

        @NotNull(message = "vatRate cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "vatRate must be zero or greater")
        BigDecimal vatRate,

        @NotNull(message = "costPrice cannot be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "costPrice must be greater than zero")
        BigDecimal costPrice,

        @Min(value = 0, message = "InitialQuantity must be greater than or equal to zero")
        Integer initialQuantity,

        @Min(value = 0, message = "minQuantity must be greater than or equal to zero")
        Integer minQuantity

) {}
