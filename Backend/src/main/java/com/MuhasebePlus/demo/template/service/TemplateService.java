package com.MuhasebePlus.demo.template.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.mapper.TemplateMapper;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import com.MuhasebePlus.demo.template.repository.TemplateUsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateUsageLogRepository usageLogRepository;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TemplateMapper templateMapper;

    // ============================================================
    // CREATE
    // ============================================================

    public TemplateResponseDto createTemplate(TemplateRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        validateUniqueCode(dto.templateCode(), companyId, null);
        validateUniqueName(dto.templateName(), companyId, null);

        Template template = templateMapper.toEntity(dto);
        template.setCompany(companyRepository.getReferenceById(companyId));
        template.setUserId(userId);

        Template saved = templateRepository.save(template);
        return templateMapper.toResponseDto(saved, 0L);
    }

    // ============================================================
    // READ
    // ============================================================

    public List<TemplateResponseDto> getAllTemplates() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Template> templates = templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(companyId);
        return mapWithUsageCounts(templates);
    }

    public List<TemplateResponseDto> getTemplatesByType(String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        TemplateType templateType = TemplateType.valueOf(type.toUpperCase());
        List<Template> templates = templateRepository.findByTemplateTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(templateType, companyId);
        return mapWithUsageCounts(templates);
    }

    public TemplateResponseDto getTemplateById(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Template template = findActiveTemplateById(templateId, companyId);
        long usageCount = usageLogRepository.countByTemplateId(templateId);
        return templateMapper.toResponseDto(template, usageCount);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public TemplateResponseDto updateTemplate(Long templateId, TemplateRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Template template = findActiveTemplateById(templateId, companyId);

        if (!template.getTemplateCode().equals(dto.templateCode())) {
            validateUniqueCode(dto.templateCode(), companyId, templateId);
        }
        if (!template.getTemplateName().equals(dto.templateName())) {
            validateUniqueName(dto.templateName(), companyId, templateId);
        }

        templateMapper.updateEntity(template, dto);
        Template updated = templateRepository.save(template);
        long usageCount = usageLogRepository.countByTemplateId(templateId);
        return templateMapper.toResponseDto(updated, usageCount);
    }

    // ============================================================
    // DELETE / RESTORE
    // ============================================================

    public void softDeleteTemplate(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Template template = findActiveTemplateById(templateId, companyId);
        template.setDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    public TemplateResponseDto restoreTemplate(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Template template = templateRepository.findByTemplateIdAndCompanyCompanyId(templateId, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        template.setDeleted(false);
        template.setDeletedAt(null);
        Template restored = templateRepository.save(template);
        long usageCount = usageLogRepository.countByTemplateId(templateId);
        return templateMapper.toResponseDto(restored, usageCount);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Template findActiveTemplateById(Long templateId, Long companyId) {
        return templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(templateId, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));
    }

    private void validateUniqueCode(String code, Long companyId, Long excludeId) {
        boolean exists;
        if (excludeId == null) {
            exists = templateRepository.existsByTemplateCodeAndCompanyCompanyIdAndIsDeletedFalse(code, companyId);
        } else {
            exists = templateRepository.existsByTemplateCodeAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(code, excludeId, companyId);
        }
        if (exists) {
            throw new RuntimeException("Bu kodda bir şablon zaten kayıtlı: " + code);
        }
    }

    private void validateUniqueName(String name, Long companyId, Long excludeId) {
        boolean exists;
        if (excludeId == null) {
            exists = templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse(name, companyId);
        } else {
            exists = templateRepository.existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(name, excludeId, companyId);
        }
        if (exists) {
            throw new RuntimeException("Bu isimde bir şablon zaten kayıtlı: " + name);
        }
    }

    private List<TemplateResponseDto> mapWithUsageCounts(List<Template> templates) {
        if (templates.isEmpty()) {
            return List.of();
        }

        List<Long> templateIds = templates.stream()
                .map(Template::getTemplateId)
                .toList();

        List<Object[]> counts = usageLogRepository.countByTemplateIds(templateIds);
        Map<Long, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return templates.stream()
                .map(t -> templateMapper.toResponseDto(t, countMap.getOrDefault(t.getTemplateId(), 0L)))
                .toList();
    }
}
