package com.MuhasebePlus.demo.fixedasset.repository;

import com.MuhasebePlus.demo.fixedasset.entity.DepreciationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepreciationLineRepository extends JpaRepository<DepreciationLine, Long> {
    List<DepreciationLine> findByFixedAssetIdOrderByPeriodYearAscPeriodMonthAsc(Long fixedAssetId);
    boolean existsByFixedAssetIdAndPeriodYearAndPeriodMonth(Long fixedAssetId, int year, int month);
}
