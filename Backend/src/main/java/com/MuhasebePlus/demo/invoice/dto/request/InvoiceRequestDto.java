package com.MuhasebePlus.demo.invoice.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceRequestDto(
    @NotBlank(message = "Invoice number cannot be blank")
    @Size(max = 50)
    String invoiceNumber,

    @NotNull(message = "Customer ID cannot be null")
    Long customerId,

    @NotNull(message = "Invoice type cannot be null")
    InvoiceType invoiceType,

    @NotNull(message = "Due date cannot be null")
    LocalDate dueDate,

    @NotEmpty(message = "At least one line item is required")
    @Valid
    List<InvoiceLineItemRequestDto> lineItems
) {
}
