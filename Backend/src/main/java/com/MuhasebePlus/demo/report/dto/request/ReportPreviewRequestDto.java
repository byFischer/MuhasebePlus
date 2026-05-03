package com.MuhasebePlus.demo.report.dto.request;

import java.time.LocalDate;

import com.MuhasebePlus.demo.report.entity.ReportType;

import jakarta.validation.constraints.NotNull;

public record ReportPreviewRequestDto(
        @NotNull(message = "Report type is required")
        ReportType reportType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
