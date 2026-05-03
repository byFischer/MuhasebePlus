package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;

public record BucketPoint(
        String label,
        BigDecimal value
) {
}
