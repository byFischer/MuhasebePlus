package com.MuhasebePlus.demo.fixedasset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisposeAssetRequestDto(
        LocalDate disposalDate,
        BigDecimal saleAmount,
        String reason
) {}
