package com.MuhasebePlus.demo.fixedasset.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.fixedasset.dto.AssetCategoryDto;
import com.MuhasebePlus.demo.fixedasset.dto.DepreciationLineDto;
import com.MuhasebePlus.demo.fixedasset.dto.FixedAssetRequestDto;
import com.MuhasebePlus.demo.fixedasset.dto.FixedAssetResponseDto;
import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import com.MuhasebePlus.demo.fixedasset.entity.DepreciationLine;
import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import com.MuhasebePlus.demo.fixedasset.repository.AssetCategoryRepository;
import com.MuhasebePlus.demo.fixedasset.repository.DepreciationLineRepository;
import com.MuhasebePlus.demo.fixedasset.repository.FixedAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FixedAssetService için kapsamlı birim testleri: kategori ve sabit kıymet CRUD,
 * varsayılan değer atamaları, soft-delete, amortisman planı ve DTO dönüşümleri.
 */
@ExtendWith(MockitoExtension.class)
class FixedAssetServiceTest {

    @Mock private FixedAssetRepository assetRepository;
    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private DepreciationLineRepository lineRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private FixedAssetService service;

    private static final Long COMPANY_ID = 2L;

    @BeforeEach
    void setUp() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    private AssetCategory category(Long id) {
        return AssetCategory.builder()
                .id(id).name("Bilgisayar").depreciationMethod("STRAIGHT_LINE")
                .usefulLifeMonths(48).annualRate(BigDecimal.valueOf(25))
                .assetAccountCode("255").depreciationAccountCode("268").build();
    }

    private FixedAsset asset(Long id, AssetCategory cat) {
        return FixedAsset.builder()
                .id(id).name("Laptop").serialNumber("SN-1").category(cat)
                .acquisitionDate(LocalDate.of(2026, 1, 1))
                .acquisitionCost(new BigDecimal("12000.00"))
                .salvageValue(new BigDecimal("0.00"))
                .accumulatedDepreciation(new BigDecimal("0.00"))
                .netBookValue(new BigDecimal("12000.00"))
                .status("ACTIVE").build();
    }

    // ── Categories ──────────────────────────────────────────────────────────────

    @Test
    void listCategories_mapsToDtos() {
        when(categoryRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(category(1L)));

        List<AssetCategoryDto> result = service.listCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Bilgisayar");
        assertThat(result.get(0).usefulLifeMonths()).isEqualTo(48);
    }

    @Test
    void createCategory_appliesDefaultsWhenNull() {
        AssetCategoryDto dto = new AssetCategoryDto(null, "Mobilya", null, null, null, null, null);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(new Company());
        when(categoryRepository.save(any(AssetCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetCategoryDto result = service.createCategory(dto);

        assertThat(result.depreciationMethod()).isEqualTo("STRAIGHT_LINE");
        assertThat(result.usefulLifeMonths()).isEqualTo(60);
        assertThat(result.name()).isEqualTo("Mobilya");
    }

    @Test
    void createCategory_usesProvidedValues() {
        AssetCategoryDto dto = new AssetCategoryDto(null, "Araç", "DECLINING_BALANCE", 120,
                BigDecimal.valueOf(20), "254", "257");
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(new Company());
        when(categoryRepository.save(any(AssetCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetCategoryDto result = service.createCategory(dto);

        assertThat(result.depreciationMethod()).isEqualTo("DECLINING_BALANCE");
        assertThat(result.usefulLifeMonths()).isEqualTo(120);
    }

    @Test
    void updateCategory_updatesFields() {
        AssetCategory existing = category(1L);
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        AssetCategoryDto dto = new AssetCategoryDto(1L, "Yeni Ad", "DECLINING_BALANCE", 36,
                BigDecimal.valueOf(30), "255", "268");

        AssetCategoryDto result = service.updateCategory(1L, dto);

        assertThat(result.name()).isEqualTo("Yeni Ad");
        assertThat(result.depreciationMethod()).isEqualTo("DECLINING_BALANCE");
        assertThat(result.usefulLifeMonths()).isEqualTo(36);
    }

    @Test
    void updateCategory_nullMethodAndMonths_keepExisting() {
        AssetCategory existing = category(1L); // STRAIGHT_LINE / 48
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        AssetCategoryDto dto = new AssetCategoryDto(1L, "Yeni Ad", null, null, null, null, null);

        AssetCategoryDto result = service.updateCategory(1L, dto);

        assertThat(result.depreciationMethod()).isEqualTo("STRAIGHT_LINE");
        assertThat(result.usefulLifeMonths()).isEqualTo(48);
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(9L,
                new AssetCategoryDto(9L, "X", null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kategori bulunamadı");
    }

    @Test
    void deleteCategory_softDeletes() {
        AssetCategory existing = category(1L);
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));

        service.deleteCategory(1L);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(categoryRepository).save(existing);
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(9L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCategory(9L))
                .isInstanceOf(BusinessException.class);
    }

    // ── Assets ────────────────────────────────────────────────────────────────

    @Test
    void listAssets_mapsWithCategoryInfo() {
        when(assetRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByNameAsc(COMPANY_ID))
                .thenReturn(List.of(asset(1L, category(3L))));

        List<FixedAssetResponseDto> result = service.listAssets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(3L);
        assertThat(result.get(0).categoryName()).isEqualTo("Bilgisayar");
        assertThat(result.get(0).depreciationMethod()).isEqualTo("STRAIGHT_LINE");
        assertThat(result.get(0).usefulLifeMonths()).isEqualTo(48);
    }

    @Test
    void getAsset_returnsDto() {
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(asset(1L, null)));

        FixedAssetResponseDto result = service.getAsset(1L);

        assertThat(result.id()).isEqualTo(1L);
        // Kategori yok → kategori alanları null
        assertThat(result.categoryId()).isNull();
        assertThat(result.categoryName()).isNull();
        assertThat(result.depreciationMethod()).isNull();
    }

    @Test
    void getAsset_notFound_throws() {
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAsset(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sabit kıymet bulunamadı");
    }

    @Test
    void createAsset_withCategoryAndDefaults() {
        AssetCategory cat = category(3L);
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(3L, COMPANY_ID))
                .thenReturn(Optional.of(cat));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(new Company());
        when(assetRepository.save(any(FixedAsset.class))).thenAnswer(inv -> inv.getArgument(0));
        // acquisitionCost ve salvageValue null → ZERO; netBookValue = cost
        FixedAssetRequestDto dto = new FixedAssetRequestDto(3L, "Sunucu", "SRV-1",
                LocalDate.of(2026, 2, 1), null, null);

        FixedAssetResponseDto result = service.createAsset(dto);

        assertThat(result.categoryId()).isEqualTo(3L);
        assertThat(result.acquisitionCost()).isEqualByComparingTo("0");
        assertThat(result.salvageValue()).isEqualByComparingTo("0");
        assertThat(result.netBookValue()).isEqualByComparingTo("0");
    }

    @Test
    void createAsset_withoutCategory() {
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(new Company());
        when(assetRepository.save(any(FixedAsset.class))).thenAnswer(inv -> inv.getArgument(0));
        FixedAssetRequestDto dto = new FixedAssetRequestDto(null, "Masa", null,
                LocalDate.of(2026, 2, 1), new BigDecimal("500.00"), new BigDecimal("50.00"));

        FixedAssetResponseDto result = service.createAsset(dto);

        assertThat(result.categoryId()).isNull();
        assertThat(result.acquisitionCost()).isEqualByComparingTo("500.00");
        assertThat(result.salvageValue()).isEqualByComparingTo("50.00");
        assertThat(result.netBookValue()).isEqualByComparingTo("500.00");
        verify(categoryRepository, never()).findByIdAndCompanyCompanyIdAndIsDeletedFalse(any(), any());
    }

    @Test
    void createAsset_categoryNotFound_throws() {
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());
        FixedAssetRequestDto dto = new FixedAssetRequestDto(99L, "X", null,
                LocalDate.now(), BigDecimal.TEN, null);

        assertThatThrownBy(() -> service.createAsset(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kategori bulunamadı");
    }

    @Test
    void updateAsset_updatesFieldsAndSalvageWhenProvided() {
        FixedAsset existing = asset(1L, null);
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(3L, COMPANY_ID))
                .thenReturn(Optional.of(category(3L)));
        when(assetRepository.save(existing)).thenReturn(existing);
        FixedAssetRequestDto dto = new FixedAssetRequestDto(3L, "Laptop Pro", "SN-2",
                LocalDate.of(2026, 3, 1), new BigDecimal("15000.00"), new BigDecimal("1000.00"));

        FixedAssetResponseDto result = service.updateAsset(1L, dto);

        assertThat(result.name()).isEqualTo("Laptop Pro");
        assertThat(result.serialNumber()).isEqualTo("SN-2");
        assertThat(result.categoryId()).isEqualTo(3L);
        assertThat(result.salvageValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void updateAsset_nullSalvage_keepsExisting() {
        FixedAsset existing = asset(1L, null); // salvage 0
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(assetRepository.save(existing)).thenReturn(existing);
        FixedAssetRequestDto dto = new FixedAssetRequestDto(null, "Yeni", null,
                LocalDate.of(2026, 3, 1), new BigDecimal("15000.00"), null);

        FixedAssetResponseDto result = service.updateAsset(1L, dto);

        assertThat(result.salvageValue()).isEqualByComparingTo("0.00");
        assertThat(result.categoryId()).isNull();
    }

    @Test
    void updateAsset_notFound_throws() {
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAsset(1L,
                new FixedAssetRequestDto(null, "X", null, LocalDate.now(), BigDecimal.TEN, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteAsset_softDeletes() {
        FixedAsset existing = asset(1L, null);
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(existing));

        service.deleteAsset(1L);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(assetRepository).save(existing);
    }

    // ── Schedule ────────────────────────────────────────────────────────────────

    @Test
    void getSchedule_mapsLinesToDtos() {
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.of(asset(1L, null)));
        DepreciationLine line = DepreciationLine.builder()
                .id(100L).periodYear(2026).periodMonth(2)
                .depreciationAmount(new BigDecimal("250.00"))
                .accumulatedDepreciation(new BigDecimal("500.00"))
                .netBookValue(new BigDecimal("11500.00"))
                .journalEntryId(77L).build();
        when(lineRepository.findByFixedAssetIdOrderByPeriodYearAscPeriodMonthAsc(1L))
                .thenReturn(List.of(line));

        List<DepreciationLineDto> result = service.getSchedule(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
        assertThat(result.get(0).periodYear()).isEqualTo(2026);
        assertThat(result.get(0).depreciationAmount()).isEqualByComparingTo("250.00");
        assertThat(result.get(0).journalEntryId()).isEqualTo(77L);
    }

    @Test
    void getSchedule_assetNotFound_throws() {
        when(assetRepository.findByIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedule(1L))
                .isInstanceOf(BusinessException.class);
    }
}
