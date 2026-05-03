package com.MuhasebePlus.demo.template.dto.request;

import com.MuhasebePlus.demo.template.entity.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record TemplateRequestDto(
        @NotBlank(message = "Template code is required")
        @Size(max = 20)
        String templateCode,

        @NotBlank(message = "Template name is required")
        @Size(max = 255)
        String templateName,

        @NotNull(message = "Template type is required")
        TemplateType templateType,

        @Size(max = 255)
        String description,

        @Size(max = 20)
        String period,

        Map<String, Object> payload
) {
}
