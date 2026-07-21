package com.MuhasebePlus.demo.dashboard.dto.request;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record WidgetDefinitionRequestDto(
        @NotBlank String name,
        String description,
        @NotNull WidgetType widgetType,
        @NotBlank String dataSource,
        Map<String, Object> queryConfig,
        Map<String, Object> visualConfig,
        Map<String, Object> config
) {}
