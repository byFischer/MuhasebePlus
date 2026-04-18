package com.MuhasebePlus.demo.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponseDto(
    Long invoiceId,
    String invoiceNumber,
    Long customerId,
    String invoiceType,
    LocalDate dueDate,
    String paymentStatus,
    BigDecimal subtotal,
    BigDecimal vatAmount,
    BigDecimal totalAmount
) {
}
