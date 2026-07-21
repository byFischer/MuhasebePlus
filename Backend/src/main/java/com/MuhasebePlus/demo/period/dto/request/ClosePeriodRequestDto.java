package com.MuhasebePlus.demo.period.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ClosePeriodRequestDto(
        @NotNull(message = "Yıl zorunludur")
        Integer year,

        @NotNull(message = "Ay zorunludur")
        @Min(value = 1, message = "Ay 1-12 arasında olmalıdır")
        @Max(value = 12, message = "Ay 1-12 arasında olmalıdır")
        Integer month
) {}
