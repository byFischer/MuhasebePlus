package com.MuhasebePlus.demo.fixedasset.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.fixedasset.dto.*;
import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import com.MuhasebePlus.demo.fixedasset.repository.AssetCategoryRepository;
import com.MuhasebePlus.demo.fixedasset.repository.DepreciationLineRepository;
import com.MuhasebePlus.demo.fixedasset.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;
    private final DepreciationLineRepository lineRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    // --- Categories ---

    @Transactional(readOnly = true)
    public List<AssetCategoryDto> listCategories() {
        Long companyId = companyContext.getCurrentCompanyId();
        return categoryRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId)
                .stream().map(this::toCategoryDto).toList();
    }

    public AssetCategoryDto createCategory(AssetCategoryDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        AssetCategory cat = AssetCategory.builder()
                .company(companyRepository.getReferenceById(companyId))
                .name(dto.name())
                .depreciationMethod(dto.depreciationMethod() != null ? dto.depreciationMethod() : "STRAIGHT_LINE")
                .usefulLifeMonths(dto.usefulLifeMonths() != null ? dto.usefulLifeMonths() : 60)
                .annualRate(dto.annualRate())
                .assetAccountCode(dto.assetAccountCode())
                .depreciationAccountCode(dto.depreciationAccountCode())
                .build();
        return toCategoryDto(categoryRepository.save(cat));
    }

    public AssetCategoryDto updateCategory(Long id, AssetCategoryDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        AssetCategory cat = categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new BusinessException("Kategori bulunamadı: " + id));
        cat.setName(dto.name());
        if (dto.depreciationMethod() != null) cat.setDepreciationMethod(dto.depreciationMethod());
        if (dto.usefulLifeMonths() != null) cat.setUsefulLifeMonths(dto.usefulLifeMonths());
        cat.setAnnualRate(dto.annualRate());
        cat.setAssetAccountCode(dto.assetAccountCode());
        cat.setDepreciationAccountCode(dto.depreciationAccountCode());
        return toCategoryDto(categoryRepository.save(cat));
    }

    public void deleteCategory(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        AssetCategory cat = categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new BusinessException("Kategori bulunamadı: " + id));
        cat.setDeleted(true);
        cat.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(cat);
    }

    // --- Assets ---

    @Transactional(readOnly = true)
    public List<FixedAssetResponseDto> listAssets() {
        Long companyId = companyContext.getCurrentCompanyId();
        return assetRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByNameAsc(companyId)
                .stream().map(this::toAssetDto).toList();
    }

    @Transactional(readOnly = true)
    public FixedAssetResponseDto getAsset(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toAssetDto(findAsset(id, companyId));
    }

    public FixedAssetResponseDto createAsset(FixedAssetRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        AssetCategory cat = dto.categoryId() != null
                ? categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(dto.categoryId(), companyId)
                        .orElseThrow(() -> new BusinessException("Kategori bulunamadı: " + dto.categoryId()))
                : null;

        BigDecimal cost = dto.acquisitionCost() != null ? dto.acquisitionCost() : BigDecimal.ZERO;
        FixedAsset asset = FixedAsset.builder()
                .company(companyRepository.getReferenceById(companyId))
                .category(cat)
                .name(dto.name())
                .serialNumber(dto.serialNumber())
                .acquisitionDate(dto.acquisitionDate())
                .acquisitionCost(cost)
                .salvageValue(dto.salvageValue() != null ? dto.salvageValue() : BigDecimal.ZERO)
                .netBookValue(cost)
                .build();
        return toAssetDto(assetRepository.save(asset));
    }

    public FixedAssetResponseDto updateAsset(Long id, FixedAssetRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        FixedAsset asset = findAsset(id, companyId);
        AssetCategory cat = dto.categoryId() != null
                ? categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(dto.categoryId(), companyId)
                        .orElseThrow(() -> new BusinessException("Kategori bulunamadı: " + dto.categoryId()))
                : null;
        asset.setCategory(cat);
        asset.setName(dto.name());
        asset.setSerialNumber(dto.serialNumber());
        asset.setAcquisitionDate(dto.acquisitionDate());
        asset.setAcquisitionCost(dto.acquisitionCost());
        if (dto.salvageValue() != null) asset.setSalvageValue(dto.salvageValue());
        return toAssetDto(assetRepository.save(asset));
    }

    public void deleteAsset(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        FixedAsset asset = findAsset(id, companyId);
        asset.setDeleted(true);
        asset.setDeletedAt(LocalDateTime.now());
        assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public List<DepreciationLineDto> getSchedule(Long assetId) {
        Long companyId = companyContext.getCurrentCompanyId();
        findAsset(assetId, companyId);
        return lineRepository.findByFixedAssetIdOrderByPeriodYearAscPeriodMonthAsc(assetId)
                .stream().map(l -> new DepreciationLineDto(
                        l.getId(), l.getPeriodYear(), l.getPeriodMonth(),
                        l.getDepreciationAmount(), l.getAccumulatedDepreciation(),
                        l.getNetBookValue(), l.getJournalEntryId()))
                .toList();
    }

    // --- Helpers ---

    FixedAsset findAsset(Long id, Long companyId) {
        return assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new BusinessException("Sabit kıymet bulunamadı: " + id));
    }

    private AssetCategoryDto toCategoryDto(AssetCategory c) {
        return new AssetCategoryDto(c.getId(), c.getName(), c.getDepreciationMethod(),
                c.getUsefulLifeMonths(), c.getAnnualRate(), c.getAssetAccountCode(), c.getDepreciationAccountCode());
    }

    FixedAssetResponseDto toAssetDto(FixedAsset a) {
        String catName = a.getCategory() != null ? a.getCategory().getName() : null;
        String method  = a.getCategory() != null ? a.getCategory().getDepreciationMethod() : null;
        Integer months = a.getCategory() != null ? a.getCategory().getUsefulLifeMonths() : null;
        return new FixedAssetResponseDto(
                a.getId(), a.getName(), a.getSerialNumber(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                catName, method, months,
                a.getAcquisitionDate(), a.getAcquisitionCost(), a.getSalvageValue(),
                a.getAccumulatedDepreciation(), a.getNetBookValue(),
                a.getStatus(), a.getDisposedAt());
    }
}
