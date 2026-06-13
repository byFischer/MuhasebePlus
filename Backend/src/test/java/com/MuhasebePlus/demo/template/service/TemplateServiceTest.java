package com.MuhasebePlus.demo.template.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.mapper.TemplateMapper;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import com.MuhasebePlus.demo.template.repository.TemplateUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TemplateService için kapsamlı birim testleri. Repository ve CompanyContext mock'lanır;
 * gerçek TemplateMapper kullanılarak dönüşüm mantığı da kapsanır.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateUsageLogRepository usageLogRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    private TemplateService service;

    private static final Long COMPANY_ID = 3L;
    private static final Long USER_ID = 9L;

    @BeforeEach
    void setUp() {
        service = new TemplateService(templateRepository, usageLogRepository,
                companyContext, companyRepository, new TemplateMapper());
    }

    private TemplateRequestDto request(String code, String name) {
        return new TemplateRequestDto(code, name, TemplateType.EXPENSE, "desc", "MONTHLY",
                Map.of("amount", 100));
    }

    private Template template(Long id, String code, String name) {
        Template t = new Template();
        t.setTemplateId(id);
        t.setTemplateCode(code);
        t.setTemplateName(name);
        t.setTemplateType(TemplateType.EXPENSE);
        return t;
    }

    // ── createTemplate ──────────────────────────────────────────────────────────

    @Test
    void createTemplate_savesAndReturnsDtoWithZeroUsage() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(templateRepository.existsByTemplateCodeAndCompanyCompanyIdAndIsDeletedFalse("TPL-1", COMPANY_ID))
                .thenReturn(false);
        when(templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse("Kira", COMPANY_ID))
                .thenReturn(false);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(new Company());
        when(templateRepository.save(any(Template.class))).thenAnswer(inv -> {
            Template t = inv.getArgument(0);
            t.setTemplateId(100L);
            return t;
        });

        TemplateResponseDto dto = service.createTemplate(request("TPL-1", "Kira"));

        assertThat(dto.templateId()).isEqualTo(100L);
        assertThat(dto.templateCode()).isEqualTo("TPL-1");
        assertThat(dto.usageCount()).isZero();
        verify(templateRepository).save(any(Template.class));
    }

    @Test
    void createTemplate_duplicateCode_throws() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(templateRepository.existsByTemplateCodeAndCompanyCompanyIdAndIsDeletedFalse("TPL-1", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createTemplate(request("TPL-1", "Kira")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bu kodda bir şablon zaten kayıtlı");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_duplicateName_throws() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(templateRepository.existsByTemplateCodeAndCompanyCompanyIdAndIsDeletedFalse("TPL-1", COMPANY_ID))
                .thenReturn(false);
        when(templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse("Kira", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createTemplate(request("TPL-1", "Kira")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bu isimde bir şablon zaten kayıtlı");
    }

    // ── getAllTemplates / getTemplatesByType ──────────────────────────────────

    @Test
    void getAllTemplates_emptyList_returnsEmpty() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.getAllTemplates()).isEmpty();
        verify(usageLogRepository, never()).countByTemplateIds(any());
    }

    @Test
    void getAllTemplates_mapsUsageCounts() {
        Template t1 = template(1L, "A", "Alfa");
        Template t2 = template(2L, "B", "Beta");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(COMPANY_ID))
                .thenReturn(List.of(t1, t2));
        // t1 → 5 kullanım, t2 → kayıt yok (0'a düşmeli)
        List<Object[]> counts = new java.util.ArrayList<>();
        counts.add(new Object[]{1L, 5L});
        when(usageLogRepository.countByTemplateIds(List.of(1L, 2L))).thenReturn(counts);

        List<TemplateResponseDto> result = service.getAllTemplates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).usageCount()).isEqualTo(5L);
        assertThat(result.get(1).usageCount()).isZero();
    }

    @Test
    void getTemplatesByType_parsesTypeCaseInsensitiveAndMaps() {
        Template t = template(1L, "A", "Alfa");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(
                TemplateType.INCOME, COMPANY_ID)).thenReturn(List.of(t));
        when(usageLogRepository.countByTemplateIds(List.of(1L))).thenReturn(List.of());

        List<TemplateResponseDto> result = service.getTemplatesByType("income");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).usageCount()).isZero();
    }

    @Test
    void getTemplatesByType_invalidType_throws() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);

        assertThatThrownBy(() -> service.getTemplatesByType("NOT_A_TYPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── getTemplateById ────────────────────────────────────────────────────────

    @Test
    void getTemplateById_returnsDtoWithUsageCount() {
        Template t = template(5L, "A", "Alfa");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(t));
        when(usageLogRepository.countByTemplateId(5L)).thenReturn(12L);

        TemplateResponseDto dto = service.getTemplateById(5L);

        assertThat(dto.templateId()).isEqualTo(5L);
        assertThat(dto.usageCount()).isEqualTo(12L);
    }

    @Test
    void getTemplateById_notFound_throws() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplateById(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template not found");
    }

    // ── updateTemplate ─────────────────────────────────────────────────────────

    @Test
    void updateTemplate_sameCodeAndName_skipsUniquenessChecks() {
        Template existing = template(5L, "TPL-1", "Kira");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(templateRepository.save(existing)).thenReturn(existing);
        when(usageLogRepository.countByTemplateId(5L)).thenReturn(3L);

        TemplateResponseDto dto = service.updateTemplate(5L, request("TPL-1", "Kira"));

        assertThat(dto.usageCount()).isEqualTo(3L);
        // Kod/isim değişmediği için benzersizlik kontrolü çağrılmamalı
        verify(templateRepository, never())
                .existsByTemplateCodeAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(any(), anyLong(), anyLong());
        verify(templateRepository, never())
                .existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(any(), anyLong(), anyLong());
    }

    @Test
    void updateTemplate_changedCode_validatesUniqueness() {
        Template existing = template(5L, "OLD", "Kira");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(templateRepository.existsByTemplateCodeAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
                "TPL-1", 5L, COMPANY_ID)).thenReturn(false);
        when(templateRepository.save(existing)).thenReturn(existing);
        when(usageLogRepository.countByTemplateId(5L)).thenReturn(0L);

        service.updateTemplate(5L, request("TPL-1", "Kira"));

        verify(templateRepository)
                .existsByTemplateCodeAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse("TPL-1", 5L, COMPANY_ID);
    }

    @Test
    void updateTemplate_changedNameDuplicate_throws() {
        Template existing = template(5L, "TPL-1", "Eski");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(templateRepository.existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
                "Kira", 5L, COMPANY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.updateTemplate(5L, request("TPL-1", "Kira")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bu isimde bir şablon zaten kayıtlı");
        verify(templateRepository, never()).save(any());
    }

    // ── softDeleteTemplate / restoreTemplate ──────────────────────────────────

    @Test
    void softDeleteTemplate_marksDeleted() {
        Template existing = template(5L, "TPL-1", "Kira");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(5L, COMPANY_ID))
                .thenReturn(Optional.of(existing));

        service.softDeleteTemplate(5L);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(templateRepository).save(existing);
    }

    @Test
    void restoreTemplate_clearsDeletionFlags() {
        Template existing = template(5L, "TPL-1", "Kira");
        existing.setDeleted(true);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyId(5L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(templateRepository.save(existing)).thenReturn(existing);
        when(usageLogRepository.countByTemplateId(5L)).thenReturn(1L);

        TemplateResponseDto dto = service.restoreTemplate(5L);

        assertThat(existing.isDeleted()).isFalse();
        assertThat(existing.getDeletedAt()).isNull();
        assertThat(dto.usageCount()).isEqualTo(1L);
    }

    @Test
    void restoreTemplate_notFound_throws() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(templateRepository.findByTemplateIdAndCompanyCompanyId(5L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restoreTemplate(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template not found");
    }
}
