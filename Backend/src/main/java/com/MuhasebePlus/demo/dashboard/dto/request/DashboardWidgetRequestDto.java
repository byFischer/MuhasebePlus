package com.MuhasebePlus.demo.dashboard.dto.request;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import jakarta.validation.constraints.NotNull;

public record DashboardWidgetRequestDto(
        @NotNull WidgetType widgetType,
        Long definitionId,
        String title,
        int positionX,
        int positionY,
        int width,
        int height,
        String config
) {}
