package com.MuhasebePlus.demo.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * TemplateMapper için saf birim testleri: DTO ↔ entity dönüşümleri ve
 * usageCount null güvenliği.
 */
class TemplateMapperTest {

    private final TemplateMapper mapper = new TemplateMapper();

    private TemplateRequestDto request() {
        return new TemplateRequestDto(
            "TPL-1",
            "Aylık Kira",
            TemplateType.EXPENSE,
            "Açıklama",
            "MONTHLY",
            Map.of("amount", 1500)
        );
    }

    @Test
    void toEntity_copiesAllFields() {
        Template t = mapper.toEntity(request());

        assertThat(t.getTemplateCode()).isEqualTo("TPL-1");
        assertThat(t.getTemplateName()).isEqualTo("Aylık Kira");
        assertThat(t.getTemplateType()).isEqualTo(TemplateType.EXPENSE);
        assertThat(t.getDescription()).isEqualTo("Açıklama");
        assertThat(t.getPeriod()).isEqualTo("MONTHLY");
        assertThat(t.getPayload()).containsEntry("amount", 1500);
    }

    @Test
    void updateEntity_overwritesExistingFields() {
        Template existing = new Template();
        existing.setTemplateCode("OLD");
        existing.setTemplateName("Eski");

        mapper.updateEntity(existing, request());

        assertThat(existing.getTemplateCode()).isEqualTo("TPL-1");
        assertThat(existing.getTemplateName()).isEqualTo("Aylık Kira");
        assertThat(existing.getTemplateType()).isEqualTo(TemplateType.EXPENSE);
    }

    @Test
    void toResponseDto_mapsFieldsAndUsageCount() {
        Template t = mapper.toEntity(request());
        t.setTemplateId(7L);

        TemplateResponseDto dto = mapper.toResponseDto(t, 42L);

        assertThat(dto.templateId()).isEqualTo(7L);
        assertThat(dto.templateCode()).isEqualTo("TPL-1");
        assertThat(dto.usageCount()).isEqualTo(42L);
    }

    @Test
    void toResponseDto_nullUsageCountDefaultsToZero() {
        Template t = mapper.toEntity(request());

        TemplateResponseDto dto = mapper.toResponseDto(t, null);

        assertThat(dto.usageCount()).isZero();
    }

    @Test
    void toResponseDto_singleArgOverloadDefaultsUsageCountToZero() {
        Template t = mapper.toEntity(request());

        TemplateResponseDto dto = mapper.toResponseDto(t);

        assertThat(dto.usageCount()).isZero();
        assertThat(dto.templateCode()).isEqualTo("TPL-1");
    }
}
