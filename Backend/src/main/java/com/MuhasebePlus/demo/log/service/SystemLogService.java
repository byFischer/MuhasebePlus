package com.MuhasebePlus.demo.log.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.dto.response.SystemLogResponseDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.log.repository.SystemLogRepository;
import com.MuhasebePlus.demo.user.entity.User;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;


    // PUBLIC METOTLAR

    public void log(LogLevel level, String details) {
        SystemLog logEntry = new SystemLog();

        Long companyId = tryGetCompanyId();
        if (companyId != null) {
            logEntry.setCompany(companyRepository.getReferenceById(companyId));
            logEntry.setUserId(tryGetUserId());
        }

        logEntry.setLogLevel(level);
        logEntry.setDetails(details);
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setIpAddress(tryGetIpAddress());

        systemLogRepository.save(logEntry);
    }

    public Page<SystemLogResponseDto> getLogs(LogLevel level, LocalDate startDate, LocalDate endDate, Long userId, Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        return systemLogRepository.findAll(buildSpec(companyId, level, userId, start, end), pageable)
                .map(this::toResponseDto);
    }

    // Tarih aralığı opsiyoneldir: Sistem Logları ekranında tarih filtresi yoktur,
    // verilmediğinde mevcut şirketin (ve varsa seviye filtresinin) tüm logları aktarılır.
    public byte[] exportLogsAsExcel(LogLevel level, LocalDate startDate, LocalDate endDate, Long userId) {
        Long companyId = companyContext.getCurrentCompanyId();
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

        List<SystemLog> logs = systemLogRepository.findAll(
                buildSpec(companyId, level, userId, start, end),
                Sort.by(Sort.Direction.DESC, "timestamp"));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sistem Loglari");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyBorders(headerStyle);

            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setWrapText(true);
            applyBorders(cellStyle);

            String[] headers = {"Log ID", "Seviye", "Tarih/Saat", "IP Adresi", "Kullanici", "Mesaj"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            int r = 1;
            for (SystemLog log : logs) {
                Row row = sheet.createRow(r++);
                set(row, 0, log.getLogId() != null ? String.valueOf(log.getLogId()) : "", cellStyle);
                set(row, 1, log.getLogLevel() != null ? log.getLogLevel().name() : "", cellStyle);
                set(row, 2, log.getTimestamp() != null ? log.getTimestamp().format(tsFmt) : "", cellStyle);
                set(row, 3, log.getIpAddress(), cellStyle);
                set(row, 4, resolveUserEmail(log), cellStyle);
                set(row, 5, log.getDetails(), cellStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(w, 2500), 18000));
            }
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel olusturulamadi: " + e.getMessage(), e);
        }
    }


    // PRIVATE METOTLAR

    private Specification<SystemLog> buildSpec(Long companyId, LogLevel level, Long userId,
                                                LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("companyId"), companyId));
            if (level != null)  predicates.add(cb.equal(root.get("logLevel"), level));
            if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
            if (start != null)  predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            if (end != null)    predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long tryGetCompanyId() {
        try {
            return companyContext.getCurrentCompanyId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Long tryGetUserId() {
        try {
            return companyContext.getCurrentUserId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String tryGetIpAddress() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private String resolveUserEmail(SystemLog log) {
        try {
            User user = log.getUser();
            return user != null ? user.getEmail() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void set(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private SystemLogResponseDto toResponseDto(SystemLog log) {
        return new SystemLogResponseDto(
                log.getLogId(),
                log.getLogLevel() != null ? log.getLogLevel().name() : null,
                log.getDetails(),
                log.getTimestamp(),
                log.getIpAddress(),
                log.getUserId(),
                resolveUserEmail(log)
        );
    }
}
