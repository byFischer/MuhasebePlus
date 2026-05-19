package com.MuhasebePlus.demo.log.controller;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.log.dto.response.SystemLogResponseDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLogResponseDto>> getLogs(
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(systemLogService.getLogs(level, startDate, endDate, userId, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {
        String csv = systemLogService.exportLogsAsCsv(level, startDate, endDate, userId);
        byte[] data = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("system_logs.csv").build());
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
