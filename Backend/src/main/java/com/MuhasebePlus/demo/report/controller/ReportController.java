package com.MuhasebePlus.demo.report.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.report.dto.request.ReportRequestDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ReportResponseDto> generateReport(@Valid @RequestBody ReportRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generateReport(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<ReportResponseDto>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ReportResponseDto> getReportById(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.getReportById(reportId));
    }

    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long reportId) {
        byte[] data = reportService.downloadReport(reportId);
        String fileName = "report_" + reportId + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        reportService.softDeleteReport(reportId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{reportId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> restoreReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.restoreReport(reportId));
    }
}
