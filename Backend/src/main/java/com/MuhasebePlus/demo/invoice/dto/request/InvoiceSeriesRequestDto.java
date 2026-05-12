package com.MuhasebePlus.demo.invoice.dto.request;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceSeriesRequestDto(
    @NotBlank(message = "Series code is required")
    String seriesCode,

    @NotNull(message = "Invoice type is required")
    InvoiceType invoiceType,

    String prefix,

    Integer year,

    boolean isActive
) {
}
