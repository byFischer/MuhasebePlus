package com.MuhasebePlus.demo.log.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class SystemLogService {

    @Autowired
    private SystemLogRepository systemLogRepository;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;


    // PUBLIC METOTLAR

    /**
     * Diger servislerin onemli olaylari kaydetmek icin cagiracagi metod.
     * Ornegin: AuthService.login(), CustomerService.create() vb.
     */
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

    public List<SystemLogResponseDto> getLogs(LogLevel level, LocalDate startDate, LocalDate endDate, Long userId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return filterLogs(companyId, level, startDate, endDate, userId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public String exportLogsAsCsv(LogLevel level, LocalDate startDate, LocalDate endDate, Long userId) {
        Long companyId = companyContext.getCurrentCompanyId();

        List<SystemLog> logs = filterLogs(companyId, level, startDate, endDate, userId);

        StringBuilder csv = new StringBuilder();
        csv.append("LogID,Level,Timestamp,IPAddress,UserID,UserEmail,Details\n");
        for (SystemLog log : logs) {
            csv.append(log.getLogId()).append(",")
               .append(log.getLogLevel() != null ? log.getLogLevel().name() : "").append(",")
               .append(log.getTimestamp()).append(",")
               .append(csvEscape(log.getIpAddress())).append(",")
               .append(log.getUserId() != null ? log.getUserId() : "").append(",")
               .append(csvEscape(resolveUserEmail(log))).append(",")
               .append(csvEscape(log.getDetails()))
               .append("\n");
        }
        return csv.toString();
    }


    // PRIVATE METOTLAR

    private List<SystemLog> filterLogs(Long companyId, LogLevel level, LocalDate startDate, LocalDate endDate, Long userId) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

        return systemLogRepository.findByCompanyCompanyIdOrderByTimestampDesc(companyId)
                .stream()
                .filter(l -> level == null || l.getLogLevel() == level)
                .filter(l -> userId == null || userId.equals(l.getUserId()))
                .filter(l -> start == null || !l.getTimestamp().isBefore(start))
                .filter(l -> end == null || !l.getTimestamp().isAfter(end))
                .toList();
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

    private String csvEscape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
