package com.MuhasebePlus.demo.accounting.repository;

import com.MuhasebePlus.demo.accounting.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {

    List<ChartOfAccount> findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(Long companyId);

    Optional<ChartOfAccount> findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(Long companyId, String accountCode);

    List<ChartOfAccount> findByCompanyCompanyIdAndParentIdIsNullAndIsDeletedFalseOrderByAccountCodeAsc(Long companyId);

    List<ChartOfAccount> findByCompanyCompanyIdAndParentIdAndIsDeletedFalseOrderByAccountCodeAsc(Long companyId, Long parentId);

    List<ChartOfAccount> findByCompanyCompanyIdAndIsLeafTrueAndIsDeletedFalseOrderByAccountCodeAsc(Long companyId);

    boolean existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(Long companyId, String accountCode);

    // Used to copy TDHP template (company_id = 0) to a new company
    List<ChartOfAccount> findByCompanyCompanyId(Long companyId);

    List<ChartOfAccount> findByCompanyCompanyIdAndAccountCodeStartingWithAndIsDeletedFalse(Long companyId, String prefix);
}
