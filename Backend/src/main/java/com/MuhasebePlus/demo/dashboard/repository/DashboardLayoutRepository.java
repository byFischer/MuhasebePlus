package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Long> {

    List<DashboardLayout> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);

    Optional<DashboardLayout> findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(Long companyId);

    boolean existsByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(Long companyId);

    Optional<DashboardLayout> findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(Long layoutId, Long companyId);
}
