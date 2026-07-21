package com.MuhasebePlus.demo.cheque.dto.request;

import jakarta.validation.constraints.NotNull;

public record CollectChequeRequestDto(
        @NotNull(message = "Banka hesabı ID zorunludur")
        Long bankAccountId
) {}
