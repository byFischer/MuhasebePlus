package com.MuhasebePlus.demo.dashboard.dto.request;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import jakarta.validation.constraints.NotNull;

public record DashboardWidgetRequestDto(
        @NotNull WidgetType widgetType,
        String title,
        int positionX,
        int positionY,
        int width,
        int height,
        String config
) {}
