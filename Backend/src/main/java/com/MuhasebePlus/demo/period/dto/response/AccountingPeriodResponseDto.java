package com.MuhasebePlus.demo.period.dto.response;

import com.MuhasebePlus.demo.period.entity.PeriodStatus;

import java.time.LocalDateTime;

public record AccountingPeriodResponseDto(
        Long periodId,
        int year,
        int month,
        PeriodStatus status,
        LocalDateTime closedAt,
        Long closedBy,
        LocalDateTime reopenedAt,
        Long reopenedBy,
        String reopenReason
) {}
