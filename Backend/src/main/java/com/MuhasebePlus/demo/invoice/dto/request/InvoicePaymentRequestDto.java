package com.MuhasebePlus.demo.invoice.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public record InvoicePaymentRequestDto(
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment date cannot be null")
    @PastOrPresent(message = "Payment date cannot be in the future")
    LocalDate paymentDate,

    @NotNull(message = "Payment method cannot be null")
    PaymentMethod paymentMethod,

    @NotNull(message = "Bank account ID cannot be null")
    Long bankAccountId,

    String notes
) {
}
