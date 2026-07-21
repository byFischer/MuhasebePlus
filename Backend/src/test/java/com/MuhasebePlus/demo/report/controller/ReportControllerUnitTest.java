package com.MuhasebePlus.demo.report.controller;

import com.MuhasebePlus.demo.report.dto.request.ReportPreviewRequestDto;
import com.MuhasebePlus.demo.report.dto.request.ReportRequestDto;
import com.MuhasebePlus.demo.report.dto.response.ReportPreviewResponseDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerUnitTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController controller;

    @Test
    void generateReport_delegatesToServiceAndReturnsCreated() {
        ReportRequestDto request = request();
        ReportResponseDto response = response(11L);
        when(reportService.generateReport(request)).thenReturn(response);

        ResponseEntity<ReportResponseDto> result = controller.generateReport(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void previewReport_delegatesToService() {
        ReportPreviewRequestDto request = new ReportPreviewRequestDto(
                ReportType.CASH_FLOW,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );
        ReportPreviewResponseDto response = new ReportPreviewResponseDto(List.of());
        when(reportService.previewReport(request)).thenReturn(response);

        ResponseEntity<ReportPreviewResponseDto> result = controller.previewReport(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void getAllAndGetById_delegateToService() {
        ReportResponseDto response = response(12L);
        when(reportService.getAllReports()).thenReturn(List.of(response));
        when(reportService.getReportById(12L)).thenReturn(response);

        assertThat(controller.getAllReports().getBody()).containsExactly(response);
        assertThat(controller.getReportById(12L).getBody()).isSameAs(response);
    }

    @Test
    void downloadReport_setsExcelHeadersAndBody() {
        byte[] bytes = new byte[] {1, 2, 3};
        when(reportService.downloadReport(44L)).thenReturn(bytes);

        ResponseEntity<byte[]> result = controller.downloadReport(44L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(result.getHeaders().getContentDisposition().getFilename()).isEqualTo("report_44.xlsx");
        assertThat(result.getHeaders().getContentLength()).isEqualTo(3);
        assertThat(result.getBody()).isEqualTo(bytes);
    }

    @Test
    void deleteAndRestore_delegateToService() {
        ReportResponseDto response = response(15L);
        when(reportService.restoreReport(15L)).thenReturn(response);

        ResponseEntity<Void> deleteResult = controller.deleteReport(15L);
        ResponseEntity<ReportResponseDto> restoreResult = controller.restoreReport(15L);

        assertThat(deleteResult.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reportService).softDeleteReport(15L);
        assertThat(restoreResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restoreResult.getBody()).isSameAs(response);
    }

    private ReportRequestDto request() {
        return new ReportRequestDto(
                ReportType.PROFIT_LOSS,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                ReportFormat.EXCEL
        );
    }

    private ReportResponseDto response(Long id) {
        return new ReportResponseDto(
                id,
                ReportType.PROFIT_LOSS.name(),
                ReportFormat.EXCEL.name(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 6, 1, 9, 0),
                1024L,
                "ada",
                false,
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 9, 5)
        );
    }
}
