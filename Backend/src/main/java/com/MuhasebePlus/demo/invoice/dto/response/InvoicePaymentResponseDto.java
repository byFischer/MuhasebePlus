package com.MuhasebePlus.demo.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;

public record InvoicePaymentResponseDto(
    Long paymentId,
    Long invoiceId,
    BigDecimal amount,
    LocalDate paymentDate,
    PaymentMethod paymentMethod,
    Long bankAccountId,
    String bankAccountName,
    String notes,
    LocalDateTime createdAt
) {
}
