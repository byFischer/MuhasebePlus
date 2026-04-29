package com.MuhasebePlus.demo.dashboard.dto.request;

import java.util.List;

public record WidgetReorderRequestDto(List<WidgetPosition> positions) {

    public record WidgetPosition(Long widgetId, int positionX, int positionY, int width, int height) {}
}
