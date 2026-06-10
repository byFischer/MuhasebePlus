package com.MuhasebePlus.demo.declaration.repository;

import com.MuhasebePlus.demo.declaration.entity.GenericDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenericDeclarationRepository extends JpaRepository<GenericDeclaration, Long> {

    List<GenericDeclaration> findByCompanyCompanyIdAndDeclarationTypeAndIsDeletedFalseOrderByYearDescPeriodDesc(
            Long companyId, String declarationType);

    Optional<GenericDeclaration> findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(Long declarationId, Long companyId);

    Optional<GenericDeclaration> findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
            Long companyId, String declarationType, Integer year, Integer period);
}
