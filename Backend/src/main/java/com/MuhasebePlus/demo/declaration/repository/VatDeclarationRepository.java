package com.MuhasebePlus.demo.declaration.repository;

import com.MuhasebePlus.demo.declaration.entity.VatDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VatDeclarationRepository extends JpaRepository<VatDeclaration, Long> {

    List<VatDeclaration> findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(Long companyId);

    Optional<VatDeclaration> findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(
            Long companyId, String declarationType, Integer year, Integer month);

    Optional<VatDeclaration> findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(Long declarationId, Long companyId);

    Optional<VatDeclaration> findTopByCompanyCompanyIdAndDeclarationTypeAndIsDeletedFalseOrderByYearDescMonthDesc(
            Long companyId, String declarationType);
}
