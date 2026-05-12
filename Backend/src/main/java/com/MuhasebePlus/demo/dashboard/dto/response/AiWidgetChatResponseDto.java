package com.MuhasebePlus.demo.dashboard.dto.response;

import com.MuhasebePlus.demo.dashboard.dto.request.WidgetDefinitionRequestDto;

import java.util.List;
import java.util.Map;

public record AiWidgetChatResponseDto(
        String assistantMessage,
        WidgetDefinitionRequestDto widgetDefinition,
        List<Map<String, Object>> previewData,
        List<Map<String, Object>> previewColumns,
        boolean previewSuccess,
        String previewError
) {}
