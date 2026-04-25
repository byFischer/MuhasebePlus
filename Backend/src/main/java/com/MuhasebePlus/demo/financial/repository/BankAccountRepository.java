package com.MuhasebePlus.demo.financial.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.financial.entity.BankAccount;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(Long accountId, Long companyId);

    Optional<BankAccount> findByAccountIdAndCompanyCompanyId(Long accountId, Long companyId);

    List<BankAccount> findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(Long companyId);

    boolean existsByIbanAndCompanyCompanyIdAndIsDeletedFalse(String iban, Long companyId);

    boolean existsByIbanAndAccountIdNotAndCompanyCompanyIdAndIsDeletedFalse(String iban, Long accountId, Long companyId);

    List<BankAccount> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
