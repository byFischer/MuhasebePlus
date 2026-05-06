package com.MuhasebePlus.demo.dashboard.dto.response;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;

import java.time.LocalDateTime;
import java.util.Map;

public record WidgetDefinitionResponseDto(
        Long definitionId,
        String name,
        String description,
        WidgetType widgetType,
        String dataSource,
        Map<String, Object> queryConfig,
        Map<String, Object> visualConfig,
        Map<String, Object> config,
        boolean isSystem,
        boolean isTemplate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
