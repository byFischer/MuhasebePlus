package com.MuhasebePlus.demo.declaration.repository;

import com.MuhasebePlus.demo.declaration.entity.BaBsDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaBsDeclarationRepository extends JpaRepository<BaBsDeclaration, Long> {

    List<BaBsDeclaration> findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(Long companyId);

    Optional<BaBsDeclaration> findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(Long declarationId, Long companyId);

    Optional<BaBsDeclaration> findByCompanyCompanyIdAndBabsTypeAndYearAndMonthAndIsDeletedFalse(
            Long companyId, String babsType, Integer year, Integer month);
}
