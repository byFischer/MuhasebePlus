package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;

public record MonthlyPoint(
        String month,
        BigDecimal income,
        BigDecimal expense
) {
}
