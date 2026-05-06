package com.MuhasebePlus.demo.customer.dto.response;

import java.time.LocalDateTime;

public record CustomerResponseDto(
    Long customerId,
    String name,
    String email,
    String taxNumber,
    String address,
    String city,
    String phoneNumber,
    String type,
    java.math.BigDecimal currentBalance,
    boolean hasOverdueInvoices,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
