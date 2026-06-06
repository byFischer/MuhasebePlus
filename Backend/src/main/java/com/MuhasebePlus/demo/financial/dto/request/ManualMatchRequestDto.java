package com.MuhasebePlus.demo.financial.dto.request;

public record ManualMatchRequestDto(
    Long statementLineId,
    Long transactionId
) {}
