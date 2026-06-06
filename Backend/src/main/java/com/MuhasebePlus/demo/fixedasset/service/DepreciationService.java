package com.MuhasebePlus.demo.fixedasset.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DepreciationService {

    private final FixedAssetRepository assetRepository;
    private final DepreciationLineRepository lineRepository;
    private final CompanyContext companyContext;
    private final FixedAssetService fixedAssetService;
    private final JournalEntryService journalEntryService;
    private final AccountingPeriodGuard periodGuard;

    /**
     * Runs monthly depreciation for all active assets of the current company.
     * Returns a summary map: {processed, skipped, totalAmount}
     */
    public Map<String, Object> runMonthlyDepreciation(int year, int month) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(LocalDate.of(year, month, 1));
        List<FixedAsset> active = assetRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(companyId, "ACTIVE");

        int processed = 0;
        int skipped = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FixedAsset asset : active) {
            if (lineRepository.existsByFixedAssetIdAndPeriodYearAndPeriodMonth(asset.getId(), year, month)) {
                skipped++;
                continue;
            }
            if (asset.getCategory() == null) {
                skipped++;
                continue;
            }

            BigDecimal depAmount = computeMonthlyDepreciation(asset);
            if (depAmount.compareTo(BigDecimal.ZERO) <= 0) {
                skipped++;
                continue;
            }

            BigDecimal newAccumulated = asset.getAccumulatedDepreciation().add(depAmount);
            BigDecimal newNetBookValue = asset.getAcquisitionCost().subtract(newAccumulated);
            if (newNetBookValue.compareTo(asset.getSalvageValue()) < 0) {
                depAmount = asset.getAcquisitionCost()
                        .subtract(asset.getAccumulatedDepreciation())
                        .subtract(asset.getSalvageValue());
                if (depAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    skipped++;
                    continue;
                }
                newAccumulated = asset.getAccumulatedDepreciation().add(depAmount);
                newNetBookValue = asset.getSalvageValue();
            }

            asset.setAccumulatedDepreciation(newAccumulated);
            asset.setNetBookValue(newNetBookValue);
            assetRepository.save(asset);

            DepreciationLine line = lineRepository.save(DepreciationLine.builder()
                    .fixedAsset(asset)
                    .periodYear(year)
                    .periodMonth(month)
                    .depreciationAmount(depAmount)
                    .accumulatedDepreciation(newAccumulated)
                    .netBookValue(newNetBookValue)
                    .build());

            Long journalEntryId = journalEntryService.createForDepreciation(line);
            if (journalEntryId != null) {
                line.setJournalEntryId(journalEntryId);
                lineRepository.save(line);
            }

            totalAmount = totalAmount.add(depAmount);
            processed++;
        }

        log.info("Depreciation run {}/{}: processed={} skipped={} total={}", year, month, processed, skipped, totalAmount);
        return Map.of("processed", processed, "skipped", skipped, "totalAmount", totalAmount);
    }

    public FixedAssetResponseDto disposeAsset(Long assetId, DisposeAssetRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        FixedAsset asset = fixedAssetService.findAsset(assetId, companyId);

        if (!"ACTIVE".equals(asset.getStatus())) {
            throw new BusinessException("Sadece aktif sabit kıymetler elden çıkarılabilir.");
        }

        asset.setStatus(dto.saleAmount() != null && dto.saleAmount().compareTo(BigDecimal.ZERO) > 0 ? "SOLD" : "DISPOSED");
        asset.setDisposedAt(dto.disposalDate());
        assetRepository.save(asset);

        return fixedAssetService.toAssetDto(asset);
    }

    // --- Private ---

    private BigDecimal computeMonthlyDepreciation(FixedAsset asset) {
        AssetCategory cat = asset.getCategory();
        BigDecimal depreciableBase = asset.getAcquisitionCost().subtract(asset.getSalvageValue());

        if ("DECLINING_BALANCE".equals(cat.getDepreciationMethod())) {
            BigDecimal rate = cat.getAnnualRate() != null ? cat.getAnnualRate() : BigDecimal.valueOf(20);
            return asset.getNetBookValue()
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
        }
        // Default: STRAIGHT_LINE
        int months = cat.getUsefulLifeMonths() > 0 ? cat.getUsefulLifeMonths() : 60;
        return depreciableBase.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
}
