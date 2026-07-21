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
    LocalDate invoiceDate,
    LocalDate dueDate,
    String paymentStatus,
    BigDecimal subtotal,
    BigDecimal vatAmount,
    BigDecimal totalAmount,
    List<InvoiceLineItemResponseDto> lineItems,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isDeleted,
    String currency,
    BigDecimal exchangeRate,
    BigDecimal totalAmountTry,
    String discountType,
    BigDecimal discountAmount,
    BigDecimal withholdingTaxAmount,
    String description,
    String deliveryAddress,
    boolean cancelled,
    String cancellationReason,
    Long referenceInvoiceId,
    String seriesCode,
    Long sequenceNumber
) {
}
