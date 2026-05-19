package com.MuhasebePlus.demo.financial.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.TransactionTemplateRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionTemplateResponseDto;
import com.MuhasebePlus.demo.financial.entity.TransactionTemplate;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionTemplateRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionTemplateService implements HardDeletable {

    private final TransactionTemplateRepository templateRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;


    // PUBLIC METOTLAR

    public TransactionTemplateResponseDto createTemplate(TransactionTemplateRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        if (templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse(dto.templateName(), companyId)) {
            throw new RuntimeException("Bu isimde bir şablon zaten kayıtlı: " + dto.templateName());
        }

        TransactionTemplate template = new TransactionTemplate();
        template.setCompany(companyRepository.getReferenceById(companyId));
        template.setUserId(userId);
        template.setTemplateName(dto.templateName());
        template.setDefaultAmount(dto.defaultAmount());
        template.setTransactionType(dto.transactionType());
        template.setDescription(dto.description());
        template.setCategory(dto.category());
        template.setDeleted(false);

        TransactionTemplate saved = templateRepository.save(template);
        return toResponseDto(saved);
    }

    public List<TransactionTemplateResponseDto> getAllTemplates() {
        Long companyId = companyContext.getCurrentCompanyId();
        return templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<TransactionTemplateResponseDto> getTemplatesByType(String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        return templateRepository
                .findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(transactionType, companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public TransactionTemplateResponseDto getTemplateById(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(findActiveTemplateById(templateId, companyId));
    }

    public TransactionTemplateResponseDto updateTemplate(Long templateId, TransactionTemplateRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        TransactionTemplate template = findActiveTemplateById(templateId, companyId);

        if (!template.getTemplateName().equals(dto.templateName()) &&
                templateRepository.existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
                        dto.templateName(), templateId, companyId)) {
            throw new RuntimeException("Bu isimde bir şablon zaten kayıtlı: " + dto.templateName());
        }

        template.setTemplateName(dto.templateName());
        template.setDefaultAmount(dto.defaultAmount());
        template.setTransactionType(dto.transactionType());
        template.setDescription(dto.description());
        template.setCategory(dto.category());

        TransactionTemplate updated = templateRepository.save(template);
        return toResponseDto(updated);
    }

    public void softDeleteTemplate(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        TransactionTemplate template = findActiveTemplateById(templateId, companyId);
        template.setDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    public TransactionTemplateResponseDto restoreTemplate(Long templateId) {
        Long companyId = companyContext.getCurrentCompanyId();
        TransactionTemplate template = templateRepository.findByTemplateIdAndCompanyCompanyId(templateId, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        template.setDeleted(false);
        template.setDeletedAt(null);
        TransactionTemplate restored = templateRepository.save(template);
        return toResponseDto(restored);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<TransactionTemplate> expired = templateRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (TransactionTemplate template : expired) {
            templateRepository.delete(template);
        }
        return expired.size();
    }


    // PRIVATE METOTLAR

    private TransactionTemplate findActiveTemplateById(Long templateId, Long companyId) {
        return templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(templateId, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));
    }

    private TransactionTemplateResponseDto toResponseDto(TransactionTemplate t) {
        return new TransactionTemplateResponseDto(
                t.getTemplateId(),
                t.getTemplateName(),
                t.getDefaultAmount(),
                t.getTransactionType() != null ? t.getTransactionType().name() : null,
                t.getDescription(),
                t.getCategory(),
                t.isDeleted(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
