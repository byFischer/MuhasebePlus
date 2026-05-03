package com.MuhasebePlus.demo.report.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponseDto(
        Long reportId,
        String reportType,
        String format,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime generatedAt,
        Long fileSize,
        String createdByUsername,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
