package com.MuhasebePlus.demo.template.dto.response;

public record TemplateApplyResponseDto(
        boolean success,
        String message,
        String targetEntityType,
        Long targetEntityId
) {
}
