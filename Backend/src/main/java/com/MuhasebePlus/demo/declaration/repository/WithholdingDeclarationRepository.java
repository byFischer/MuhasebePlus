package com.MuhasebePlus.demo.declaration.repository;

import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WithholdingDeclarationRepository extends JpaRepository<WithholdingDeclaration, Long> {

    List<WithholdingDeclaration> findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(Long companyId);

    Optional<WithholdingDeclaration> findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(Long declarationId, Long companyId);

    Optional<WithholdingDeclaration> findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(
            Long companyId, Integer year, Integer month);
}
