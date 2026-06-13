package com.MuhasebePlus.demo.report;

import com.MuhasebePlus.demo.report.dto.request.ReportPreviewRequestDto;
import com.MuhasebePlus.demo.report.dto.request.ReportRequestDto;
import com.MuhasebePlus.demo.report.dto.response.BucketPoint;
import com.MuhasebePlus.demo.report.dto.response.KpiItem;
import com.MuhasebePlus.demo.report.dto.response.MonthlyPoint;
import com.MuhasebePlus.demo.report.dto.response.PreviewSection;
import com.MuhasebePlus.demo.report.dto.response.ReconciliationItemDto;
import com.MuhasebePlus.demo.report.dto.response.ReportPreviewResponseDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.dto.response.StatementLineDto;
import com.MuhasebePlus.demo.report.dto.response.StatementResponseDto;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.mapper.ReportMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDtoEntityTest {

    @Test
    void reportRequestAndResponseRecordsExposeValues() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        ReportRequestDto request = new ReportRequestDto(ReportType.PROFIT_LOSS, start, end, ReportFormat.EXCEL);
        ReportPreviewRequestDto previewRequest = new ReportPreviewRequestDto(ReportType.CASH_FLOW, start, end);
        ReportResponseDto response = new ReportResponseDto(
                1L,
                "PROFIT_LOSS",
                "EXCEL",
                start,
                end,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                128L,
                "ada",
                false,
                LocalDateTime.of(2026, 6, 1, 10, 1),
                LocalDateTime.of(2026, 6, 1, 10, 2)
        );

        assertThat(request.reportType()).isEqualTo(ReportType.PROFIT_LOSS);
        assertThat(previewRequest.reportType()).isEqualTo(ReportType.CASH_FLOW);
        assertThat(response.createdByUsername()).isEqualTo("ada");
        assertThat(new ReportMapper()).isNotNull();
    }

    @Test
    void previewAndStatementRecordsExposeValues() {
        KpiItem kpi = new KpiItem("Gelir", new BigDecimal("100.00"), "currency", "good");
        MonthlyPoint monthly = new MonthlyPoint("2026-01", new BigDecimal("100.00"), new BigDecimal("40.00"));
        BucketPoint bucket = new BucketPoint("0-30", new BigDecimal("25.00"));
        PreviewSection section = new PreviewSection(
                "Ozet",
                List.of(kpi),
                new BigDecimal("60.0"),
                "Kar marji",
                List.of(monthly),
                List.of(bucket)
        );
        ReportPreviewResponseDto preview = new ReportPreviewResponseDto(List.of(section));
        StatementLineDto line = new StatementLineDto(
                LocalDate.of(2026, 5, 1),
                "Satis",
                "S-1",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00")
        );
        StatementResponseDto statement = new StatementResponseDto(
                7L,
                "Ada Ltd.",
                "1234567890",
                "120.01.001",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                List.of(line)
        );
        ReconciliationItemDto reconciliationItem = new ReconciliationItemDto(
                "S-1",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 20),
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("60.00"),
                5
        );

        assertThat(preview.sections()).containsExactly(section);
        assertThat(statement.lines()).containsExactly(line);
        assertThat(reconciliationItem.remainingAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void reportEntityStoresMetadata() {
        Report report = new Report();
        report.setReportId(4L);
        report.setReportType(ReportType.BALANCE_SHEET);
        report.setFormat(ReportFormat.EXCEL);
        report.setStartDate(LocalDate.of(2026, 1, 1));
        report.setEndDate(LocalDate.of(2026, 12, 31));
        report.setGeneratedAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        report.setFilePath("target/test-reports/report.xlsx");
        report.setFileSize(2048L);
        report.setUserId(9L);
        report.setDeleted(true);

        assertThat(report.getReportId()).isEqualTo(4L);
        assertThat(report.getReportType()).isEqualTo(ReportType.BALANCE_SHEET);
        assertThat(report.getFormat()).isEqualTo(ReportFormat.EXCEL);
        assertThat(report.getFilePath()).endsWith("report.xlsx");
        assertThat(report.getFileSize()).isEqualTo(2048L);
        assertThat(report.getUserId()).isEqualTo(9L);
        assertThat(report.isDeleted()).isTrue();
    }
}
