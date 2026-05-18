package com.MuhasebePlus.demo.cheque.dto.request;

import jakarta.validation.constraints.NotNull;

public record EndorseChequeRequestDto(
        @NotNull(message = "Ciro alıcısı müşteri ID zorunludur")
        Long toCustomerId
) {}
