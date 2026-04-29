package com.MuhasebePlus.demo.dashboard.dto.response;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;

import java.time.LocalDateTime;

public record DashboardWidgetResponseDto(
        Long widgetId,
        WidgetType widgetType,
        String title,
        int positionX,
        int positionY,
        int width,
        int height,
        String config,
        LocalDateTime createdAt
) {}
