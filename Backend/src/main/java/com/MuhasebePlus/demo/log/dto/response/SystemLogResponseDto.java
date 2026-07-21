package com.MuhasebePlus.demo.log.dto.response;

import java.time.LocalDateTime;

public record SystemLogResponseDto(
        Long logId,
        String logLevel,
        String details,
        LocalDateTime timestamp,
        String ipAddress,
        Long userId,
        String userEmail
) {
}
