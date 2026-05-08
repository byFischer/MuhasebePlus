package com.MuhasebePlus.demo.dashboard.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardLayoutResponseDto(
        Long layoutId,
        String name,
        boolean isDefault,
        String theme,
        String accentColor,
        String layoutPreset,
        List<DashboardWidgetResponseDto> widgets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
