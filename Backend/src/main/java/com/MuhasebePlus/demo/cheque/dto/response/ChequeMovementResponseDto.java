package com.MuhasebePlus.demo.cheque.dto.response;

import com.MuhasebePlus.demo.cheque.entity.ChequeMovementType;

import java.time.LocalDate;

public record ChequeMovementResponseDto(
        Long movementId,
        ChequeMovementType movementType,
        String sourceType,
        LocalDate movementDate,
        Long bankAccountId,
        Long transactionId,
        String reason
) {}
