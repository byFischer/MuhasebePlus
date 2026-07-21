package com.MuhasebePlus.demo.template.mapper;

import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {

    public Template toEntity(TemplateRequestDto dto) {
        Template template = new Template();
        template.setTemplateCode(dto.templateCode());
        template.setTemplateName(dto.templateName());
        template.setTemplateType(dto.templateType());
        template.setDescription(dto.description());
        template.setPeriod(dto.period());
        template.setPayload(dto.payload());
        return template;
    }

    public void updateEntity(Template template, TemplateRequestDto dto) {
        template.setTemplateCode(dto.templateCode());
        template.setTemplateName(dto.templateName());
        template.setTemplateType(dto.templateType());
        template.setDescription(dto.description());
        template.setPeriod(dto.period());
        template.setPayload(dto.payload());
    }

    public TemplateResponseDto toResponseDto(Template template, Long usageCount) {
        return new TemplateResponseDto(
                template.getTemplateId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getTemplateType(),
                template.getDescription(),
                template.getPeriod(),
                template.getPayload(),
                usageCount != null ? usageCount : 0L,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public TemplateResponseDto toResponseDto(Template template) {
        return toResponseDto(template, 0L);
    }
}
