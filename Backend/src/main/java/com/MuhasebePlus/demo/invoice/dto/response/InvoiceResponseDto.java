package com.MuhasebePlus.demo.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponseDto(
    Long invoiceId,
    String invoiceNumber,
    Long customerId,
    String customerName,
    String invoiceType,
    LocalDate dueDate,
    String paymentStatus,
    BigDecimal subtotal,
    BigDecimal vatAmount,
    BigDecimal totalAmount,
    List<InvoiceLineItemResponseDto> lineItems,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
