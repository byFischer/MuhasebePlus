package com.MuhasebePlus.demo.invoice.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

    @NotNull(message = "Payment status cannot be blank")
    PaymentStatus paymentStatus,

    @NotNull(message = "Subtotal cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be greater than zero")
    BigDecimal subtotal,

    @NotNull(message = "VAT amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "VAT amount must be zero or greater")
    BigDecimal vatAmount,

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than zero")
    BigDecimal totalAmount
) {
    
}
