package com.MuhasebePlus.demo.financial.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.financial.entity.TransactionTemplate;
import com.MuhasebePlus.demo.financial.entity.TransactionType;

@Repository
public interface TransactionTemplateRepository extends JpaRepository<TransactionTemplate, Long> {

    Optional<TransactionTemplate> findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(Long templateId, Long companyId);

    Optional<TransactionTemplate> findByTemplateIdAndCompanyCompanyId(Long templateId, Long companyId);

    List<TransactionTemplate> findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(Long companyId);

    List<TransactionTemplate> findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(
            TransactionType transactionType, Long companyId);

    boolean existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse(String templateName, Long companyId);

    boolean existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
            String templateName, Long templateId, Long companyId);

    List<TransactionTemplate> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
