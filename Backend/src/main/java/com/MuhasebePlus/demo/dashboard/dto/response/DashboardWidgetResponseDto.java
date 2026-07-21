package com.MuhasebePlus.demo.dashboard.dto.response;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;

import java.time.LocalDateTime;
import java.util.Map;

public record DashboardWidgetResponseDto(
        Long widgetId,
        WidgetType widgetType,
        Long definitionId,
        String title,
        Integer slotIndex,
        int positionX,
        int positionY,
        int width,
        int height,
        String config,
        String dataSource,
        Map<String, Object> queryConfig,
        Map<String, Object> visualConfig,
        LocalDateTime createdAt
) {}
