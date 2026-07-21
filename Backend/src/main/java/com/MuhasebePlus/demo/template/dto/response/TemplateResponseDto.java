package com.MuhasebePlus.demo.template.dto.response;

import com.MuhasebePlus.demo.template.entity.TemplateType;

import java.time.LocalDateTime;
import java.util.Map;

public record TemplateResponseDto(
        Long templateId,
        String templateCode,
        String templateName,
        TemplateType templateType,
        String description,
        String period,
        Map<String, Object> payload,
        Long usageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
