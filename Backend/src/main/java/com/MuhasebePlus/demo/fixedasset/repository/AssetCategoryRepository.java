package com.MuhasebePlus.demo.fixedasset.repository;

import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    List<AssetCategory> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);
    Optional<AssetCategory> findByIdAndCompanyCompanyIdAndIsDeletedFalse(Long id, Long companyId);
}
