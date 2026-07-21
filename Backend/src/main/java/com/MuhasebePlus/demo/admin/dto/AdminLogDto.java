package com.MuhasebePlus.demo.admin.dto;

import com.MuhasebePlus.demo.log.entity.SystemLog;

import java.time.LocalDateTime;

public record AdminLogDto(
        Long logId,
        String logLevel,
        String details,
        LocalDateTime timestamp,
        String ipAddress,
        Long userId,
        String userEmail,
        Long companyId,
        String companyName
) {

    public static AdminLogDto fromEntity(SystemLog log) {
        return new AdminLogDto(
                log.getLogId(),
                log.getLogLevel() != null ? log.getLogLevel().name() : null,
                log.getDetails(),
                log.getTimestamp(),
                log.getIpAddress(),
                log.getUserId(),
                resolveUserEmail(log),
                log.getCompany() != null ? log.getCompany().getCompanyId() : null,
                resolveCompanyName(log)
        );
    }

    private static String resolveUserEmail(SystemLog log) {
        try {
            return log.getUser() != null ? log.getUser().getEmail() : null;
        } catch (RuntimeException e) {
            // Kullanıcı silinmişse lazy proxy çözülemeyebilir
            return null;
        }
    }

    private static String resolveCompanyName(SystemLog log) {
        try {
            return log.getCompany() != null ? log.getCompany().getCompanyName() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
