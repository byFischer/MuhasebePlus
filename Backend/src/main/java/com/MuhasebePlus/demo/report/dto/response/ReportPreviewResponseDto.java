package com.MuhasebePlus.demo.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReportPreviewResponseDto(
       List<PreviewSection> sections
) {
}
