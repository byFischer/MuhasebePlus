package com.MuhasebePlus.demo.invoice.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.MuhasebePlus.demo.invoice.entity.DiscountType;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.financial.entity.Currency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvoiceRequestDto(
    @NotBlank(message = "Invoice number cannot be blank")
    @Size(max = 50)
    String invoiceNumber,

    @NotNull(message = "Customer ID cannot be null")
    Long customerId,

    @NotNull(message = "Invoice type cannot be null")
    InvoiceType invoiceType,

    @NotNull(message = "Invoice date cannot be null")
    @PastOrPresent(message = "Invoice date cannot be in the future")
    LocalDate invoiceDate,

    @NotNull(message = "Due date cannot be null")
    LocalDate dueDate,

    @NotEmpty(message = "At least one line item is required")
    @Valid
    List<InvoiceLineItemRequestDto> lineItems,

    Currency currency,

    BigDecimal exchangeRate,

    DiscountType discountType,

    @PositiveOrZero(message = "Discount amount must be zero or positive")
    BigDecimal discountAmount,

    @Size(max = 500)
    String description,

    @Size(max = 500)
    String deliveryAddress
) {
}
