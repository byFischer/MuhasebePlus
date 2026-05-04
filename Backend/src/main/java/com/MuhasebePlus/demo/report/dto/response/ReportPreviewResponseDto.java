package com.MuhasebePlus.demo.report.dto.response;


import java.util.List;

public record ReportPreviewResponseDto(
       List<PreviewSection> sections
) {
}
