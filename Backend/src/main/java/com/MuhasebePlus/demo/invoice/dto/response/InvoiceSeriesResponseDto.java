package com.MuhasebePlus.demo.invoice.dto.response;

import java.time.LocalDateTime;

public record InvoiceSeriesResponseDto(
    Long seriesId,
    String seriesCode,
    String invoiceType,
    String prefix,
    Integer year,
    Long lastSequenceNumber,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
