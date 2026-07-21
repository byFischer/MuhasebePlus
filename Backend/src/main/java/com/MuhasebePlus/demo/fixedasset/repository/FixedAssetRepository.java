package com.MuhasebePlus.demo.fixedasset.repository;

import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByCompanyCompanyIdAndIsDeletedFalseOrderByNameAsc(Long companyId);
    List<FixedAsset> findByCompanyCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);
    Optional<FixedAsset> findByIdAndCompanyCompanyIdAndIsDeletedFalse(Long id, Long companyId);
}
