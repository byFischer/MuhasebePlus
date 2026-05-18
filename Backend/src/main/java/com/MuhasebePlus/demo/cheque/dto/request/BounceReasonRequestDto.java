package com.MuhasebePlus.demo.cheque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BounceReasonRequestDto(
        @NotBlank(message = "Karşılıksız sebebi zorunludur")
        @Size(max = 500)
        String reason
) {}
