package com.MuhasebePlus.demo.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    BigDecimal currentBalance,
    boolean hasOverdueInvoices,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String accountCode,
    BigDecimal openingBalance,
    LocalDate openingBalanceDate,
    String taxOffice,
    String identityNumber,
    String iban,
    String currency,
    BigDecimal creditLimit,
    String customerRole
) {
}
