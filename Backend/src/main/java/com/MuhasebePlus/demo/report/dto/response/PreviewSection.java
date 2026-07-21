package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PreviewSection(
        String title,
        List<KpiItem> kpis,
        BigDecimal progressValue,
        String progressLabel,
        List<MonthlyPoint> monthlySeries,
        List<BucketPoint> buckets
) {}