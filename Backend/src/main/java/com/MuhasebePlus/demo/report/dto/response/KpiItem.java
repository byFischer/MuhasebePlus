package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;

public record KpiItem(
        String label,
        BigDecimal value,
        String format,
        String tone
) {
}
