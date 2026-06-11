package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminLogDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.log.repository.SystemLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tüm şirketlerin loglarını kapsayan (cross-tenant) log sorguları.
 * Tenant-scoped versiyon için {@code SystemLogService} kullanılır.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminLogService {

    private final SystemLogRepository systemLogRepository;

    public Page<AdminLogDto> getLogs(Long companyId, LogLevel level, Long userId, boolean auditOnly,
                                     LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        return systemLogRepository.findAll(buildSpec(companyId, level, userId, auditOnly, start, end), pageable)
                .map(AdminLogDto::fromEntity);
    }

    public String exportLogsAsCsv(Long companyId, LogLevel level, Long userId, boolean auditOnly,
                                  LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Export için başlangıç ve bitiş tarihi zorunludur.");
        }
        List<SystemLog> logs = systemLogRepository.findAll(
                buildSpec(companyId, level, userId, auditOnly, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)));

        StringBuilder csv = new StringBuilder();
        csv.append("LogID,Level,Timestamp,IPAddress,CompanyID,CompanyName,UserID,UserEmail,Details\n");
        for (SystemLog log : logs) {
            AdminLogDto dto = AdminLogDto.fromEntity(log);
            csv.append(dto.logId()).append(",")
               .append(dto.logLevel() != null ? dto.logLevel() : "").append(",")
               .append(dto.timestamp()).append(",")
               .append(csvEscape(dto.ipAddress())).append(",")
               .append(dto.companyId() != null ? dto.companyId() : "").append(",")
               .append(csvEscape(dto.companyName())).append(",")
               .append(dto.userId() != null ? dto.userId() : "").append(",")
               .append(csvEscape(dto.userEmail())).append(",")
               .append(csvEscape(dto.details()))
               .append("\n");
        }
        return csv.toString();
    }


    // PRIVATE METOTLAR

    private Specification<SystemLog> buildSpec(Long companyId, LogLevel level, Long userId, boolean auditOnly,
                                               LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (companyId != null) predicates.add(cb.equal(root.get("company").get("companyId"), companyId));
            if (level != null)     predicates.add(cb.equal(root.get("logLevel"), level));
            if (userId != null)    predicates.add(cb.equal(root.get("userId"), userId));
            if (auditOnly)         predicates.add(cb.like(root.get("details"), AdminAuditService.AUDIT_PREFIX + "%"));
            if (start != null)     predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            if (end != null)       predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
