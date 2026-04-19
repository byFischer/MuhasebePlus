package com.MuhasebePlus.demo.customer.dto.response;

import java.time.LocalDateTime;

public record CustomerResponseDto(
    Long customerId,
    String name,
    String taxNumber,
    String address,
    String city,
    String phoneNumber,
    String type, // enum.name()
    java.math.BigDecimal currentBalance,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) 
{    
}
