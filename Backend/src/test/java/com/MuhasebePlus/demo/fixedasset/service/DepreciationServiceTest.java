package com.MuhasebePlus.demo.fixedasset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.fixedasset.dto.DisposeAssetRequestDto;
import com.MuhasebePlus.demo.fixedasset.dto.FixedAssetResponseDto;
import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import com.MuhasebePlus.demo.fixedasset.entity.DepreciationLine;
import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import com.MuhasebePlus.demo.fixedasset.repository.DepreciationLineRepository;
import com.MuhasebePlus.demo.fixedasset.repository.FixedAssetRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DepreciationService için kapsamlı birim testleri: aylık amortisman koşusu
 * (straight-line / declining-balance hesabı, atlama dalları, hurda değeri sınırı,
 * yevmiye kaydı bağlama) ve elden çıkarma akışları.
 */
@ExtendWith(MockitoExtension.class)
class DepreciationServiceTest {

    @Mock
    private FixedAssetRepository assetRepository;

    @Mock
    private DepreciationLineRepository lineRepository;

    @Mock
    private CompanyContext companyContext;

    @Mock
    private FixedAssetService fixedAssetService;

    @Mock
    private JournalEntryService journalEntryService;

    @Mock
    private AccountingPeriodGuard periodGuard;

    @InjectMocks
    private DepreciationService service;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient()
            .when(companyContext.getCurrentCompanyId())
            .thenReturn(COMPANY_ID);
    }

    private AssetCategory cat(
        String method,
        int months,
        BigDecimal annualRate
    ) {
        return AssetCategory.builder()
            .id(1L)
            .name("Kat")
            .depreciationMethod(method)
            .usefulLifeMonths(months)
            .annualRate(annualRate)
            .build();
    }

    private FixedAsset asset(
        Long id,
        AssetCategory category,
        String cost,
        String salvage,
        String accumulated,
        String netBookValue
    ) {
        return FixedAsset.builder()
            .id(id)
            .name("Asset")
            .category(category)
            .status("ACTIVE")
            .acquisitionDate(LocalDate.of(2025, 1, 1))
            .acquisitionCost(new BigDecimal(cost))
            .salvageValue(new BigDecimal(salvage))
            .accumulatedDepreciation(new BigDecimal(accumulated))
            .netBookValue(new BigDecimal(netBookValue))
            .build();
    }

    // ── runMonthlyDepreciation: hesap yöntemleri ──────────────────────────────

    @Test
    void runMonthly_straightLine_processesAndBindsJournalEntry() {
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "12000.00",
            "0.00",
            "0.00",
            "12000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);
        when(lineRepository.save(any(DepreciationLine.class))).thenAnswer(inv ->
            inv.getArgument(0)
        );
        when(
            journalEntryService.createForDepreciation(
                any(DepreciationLine.class)
            )
        ).thenReturn(77L);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("processed")).isEqualTo(1);
        assertThat(result.get("skipped")).isEqualTo(0);
        // (12000 - 0) / 48 = 250.00
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo(
            "250.00"
        );
        assertThat(a.getAccumulatedDepreciation()).isEqualByComparingTo(
            "250.00"
        );
        assertThat(a.getNetBookValue()).isEqualByComparingTo("11750.00");
        verify(periodGuard).assertOpen(LocalDate.of(2026, 2, 1));
        // Yevmiye id set edildiği için satır iki kez kaydedilir
        verify(lineRepository, times(2)).save(any(DepreciationLine.class));
        verify(journalEntryService).createForDepreciation(
            any(DepreciationLine.class)
        );
    }

    @Test
    void runMonthly_decliningBalance_usesNetBookValueAndRate() {
        FixedAsset a = asset(
            1L,
            cat("DECLINING_BALANCE", 48, BigDecimal.valueOf(20)),
            "12000.00",
            "0.00",
            "0.00",
            "12000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);
        when(lineRepository.save(any(DepreciationLine.class))).thenAnswer(inv ->
            inv.getArgument(0)
        );
        when(
            journalEntryService.createForDepreciation(
                any(DepreciationLine.class)
            )
        ).thenReturn(null);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        // 12000 * 20 / 1200 = 200.00
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo(
            "200.00"
        );
        // Yevmiye id null → satır yalnızca bir kez kaydedilir
        verify(lineRepository, times(1)).save(any(DepreciationLine.class));
    }

    @Test
    void runMonthly_decliningBalance_nullRateDefaultsTo20() {
        FixedAsset a = asset(
            1L,
            cat("DECLINING_BALANCE", 48, null),
            "12000.00",
            "0.00",
            "0.00",
            "10000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                anyLong(),
                anyInt(),
                anyInt()
            )
        ).thenReturn(false);
        when(lineRepository.save(any(DepreciationLine.class))).thenAnswer(inv ->
            inv.getArgument(0)
        );
        when(
            journalEntryService.createForDepreciation(
                any(DepreciationLine.class)
            )
        ).thenReturn(1L);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 3);

        // netBookValue 10000 * 20 / 1200 = 166.67
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo(
            "166.67"
        );
    }

    // ── runMonthlyDepreciation: atlama dalları ────────────────────────────────

    @Test
    void runMonthly_skipsWhenLineAlreadyExists() {
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "12000.00",
            "0.00",
            "0.00",
            "12000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(true);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("processed")).isEqualTo(0);
        assertThat(result.get("skipped")).isEqualTo(1);
        verify(lineRepository, never()).save(any());
    }

    @Test
    void runMonthly_skipsWhenCategoryNull() {
        FixedAsset a = asset(1L, null, "12000.00", "0.00", "0.00", "12000.00");
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("skipped")).isEqualTo(1);
        assertThat(result.get("processed")).isEqualTo(0);
    }

    @Test
    void runMonthly_skipsWhenDepreciationNonPositive() {
        // cost == salvage → depreciableBase 0 → depAmount 0 → atlanır
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "5000.00",
            "5000.00",
            "0.00",
            "5000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("skipped")).isEqualTo(1);
        verify(lineRepository, never()).save(any());
    }

    @Test
    void runMonthly_capsAtSalvageValue() {
        // cost 1000, salvage 900, accumulated 99 → ham depAmount=100/48=2.08
        // newNBV=1000-101.08=898.92 < 900 → sınırlanır: depAmount=1000-99-900=1.00, NBV=900
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "1000.00",
            "900.00",
            "99.00",
            "901.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);
        when(lineRepository.save(any(DepreciationLine.class))).thenAnswer(inv ->
            inv.getArgument(0)
        );
        when(journalEntryService.createForDepreciation(any())).thenReturn(1L);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("processed")).isEqualTo(1);
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo(
            "1.00"
        );
        assertThat(a.getNetBookValue()).isEqualByComparingTo("900.00");
        assertThat(a.getAccumulatedDepreciation()).isEqualByComparingTo(
            "100.00"
        );
    }

    @Test
    void runMonthly_skipsWhenCappedAmountNonPositive() {
        // accumulated zaten cost-salvage seviyesinde → sınırlama sonrası depAmount<=0 → atlanır
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "1000.00",
            "900.00",
            "100.00",
            "900.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                2
            )
        ).thenReturn(false);

        Map<String, Object> result = service.runMonthlyDepreciation(2026, 2);

        assertThat(result.get("skipped")).isEqualTo(1);
        assertThat(result.get("processed")).isEqualTo(0);
        verify(lineRepository, never()).save(any());
    }

    @Test
    void runMonthly_closedPeriod_propagatesGuardException() {
        doThrow(new BusinessException("Dönem kapalı"))
            .when(periodGuard)
            .assertOpen(any(LocalDate.class));

        assertThatThrownBy(() -> service.runMonthlyDepreciation(2026, 2))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Dönem kapalı");
        verify(
            assetRepository,
            never()
        ).findByCompanyCompanyIdAndStatusAndIsDeletedFalse(any(), any());
    }

    @Test
    void runMonthly_capturesLinePeriodAndAmounts() {
        FixedAsset a = asset(
            1L,
            cat("STRAIGHT_LINE", 48, null),
            "12000.00",
            "0.00",
            "0.00",
            "12000.00"
        );
        when(
            assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                COMPANY_ID,
                "ACTIVE"
            )
        ).thenReturn(List.of(a));
        when(
            lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(
                1L,
                2026,
                5
            )
        ).thenReturn(false);
        when(lineRepository.save(any(DepreciationLine.class))).thenAnswer(inv ->
            inv.getArgument(0)
        );
        when(journalEntryService.createForDepreciation(any())).thenReturn(9L);

        service.runMonthlyDepreciation(2026, 5);

        ArgumentCaptor<DepreciationLine> captor = ArgumentCaptor.forClass(
            DepreciationLine.class
        );
        verify(lineRepository, times(2)).save(captor.capture());
        DepreciationLine saved = captor.getAllValues().get(0);
        assertThat(saved.getPeriodYear()).isEqualTo(2026);
        assertThat(saved.getPeriodMonth()).isEqualTo(5);
        assertThat(saved.getDepreciationAmount()).isEqualByComparingTo(
            "250.00"
        );
    }

    // ── disposeAsset ──────────────────────────────────────────────────────────

    @Test
    void disposeAsset_withSaleAmount_marksSold() {
        FixedAsset a = asset(1L, null, "1000.00", "0.00", "0.00", "1000.00");
        when(fixedAssetService.findAsset(1L, COMPANY_ID)).thenReturn(a);
        when(fixedAssetService.toAssetDto(a)).thenReturn(mockDto());
        DisposeAssetRequestDto dto = new DisposeAssetRequestDto(
            LocalDate.of(2026, 6, 1),
            new BigDecimal("500.00"),
            "Satıldı"
        );

        service.disposeAsset(1L, dto);

        assertThat(a.getStatus()).isEqualTo("SOLD");
        assertThat(a.getDisposedAt()).isEqualTo(LocalDate.of(2026, 6, 1));
        verify(assetRepository).save(a);
    }

    @Test
    void disposeAsset_withoutSaleAmount_marksDisposed() {
        FixedAsset a = asset(1L, null, "1000.00", "0.00", "0.00", "1000.00");
        when(fixedAssetService.findAsset(1L, COMPANY_ID)).thenReturn(a);
        when(fixedAssetService.toAssetDto(a)).thenReturn(mockDto());
        DisposeAssetRequestDto dto = new DisposeAssetRequestDto(
            LocalDate.of(2026, 6, 1),
            null,
            "Hurda"
        );

        service.disposeAsset(1L, dto);

        assertThat(a.getStatus()).isEqualTo("DISPOSED");
    }

    @Test
    void disposeAsset_zeroSaleAmount_marksDisposed() {
        FixedAsset a = asset(1L, null, "1000.00", "0.00", "0.00", "1000.00");
        when(fixedAssetService.findAsset(1L, COMPANY_ID)).thenReturn(a);
        when(fixedAssetService.toAssetDto(a)).thenReturn(mockDto());
        DisposeAssetRequestDto dto = new DisposeAssetRequestDto(
            LocalDate.now(),
            BigDecimal.ZERO,
            null
        );

        service.disposeAsset(1L, dto);

        assertThat(a.getStatus()).isEqualTo("DISPOSED");
    }

    @Test
    void disposeAsset_notActive_throws() {
        FixedAsset a = asset(1L, null, "1000.00", "0.00", "0.00", "1000.00");
        a.setStatus("SOLD");
        when(fixedAssetService.findAsset(1L, COMPANY_ID)).thenReturn(a);
        DisposeAssetRequestDto dto = new DisposeAssetRequestDto(
            LocalDate.now(),
            null,
            null
        );

        assertThatThrownBy(() -> service.disposeAsset(1L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Sadece aktif sabit kıymetler");
        verify(assetRepository, never()).save(any());
    }

    private FixedAssetResponseDto mockDto() {
        return new FixedAssetResponseDto(
            1L,
            "Asset",
            null,
            null,
            null,
            null,
            null,
            LocalDate.now(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "DISPOSED",
            LocalDate.now()
        );
    }
}
