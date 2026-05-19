package com.MuhasebePlus.demo.log.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public String exportLogsAsCsv(LogLevel level, LocalDate startDate, LocalDate endDate, Long userId) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Export için başlangıç ve bitiş tarihi zorunludur.");
        }
        Long companyId = companyContext.getCurrentCompanyId();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<SystemLog> logs = systemLogRepository.findAll(buildSpec(companyId, level, userId, start, end));

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
