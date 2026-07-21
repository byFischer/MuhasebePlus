package com.MuhasebePlus.demo.period.dto.request;

import jakarta.validation.constraints.*;

public record ReopenPeriodRequestDto(
        @NotNull(message = "Yıl zorunludur")
        Integer year,

        @NotNull(message = "Ay zorunludur")
        @Min(value = 1, message = "Ay 1-12 arasında olmalıdır")
        @Max(value = 12, message = "Ay 1-12 arasında olmalıdır")
        Integer month,

        @NotBlank(message = "Yeniden açma sebebi zorunludur")
        @Size(max = 500, message = "Sebep en fazla 500 karakter olabilir")
        String reason
) {}
