package com.MuhasebePlus.demo.template.repository;

import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(Long companyId);

    List<Template> findByTemplateTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(
            TemplateType templateType, Long companyId);

    Optional<Template> findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(Long templateId, Long companyId);

    Optional<Template> findByTemplateIdAndCompanyCompanyId(Long templateId, Long companyId);

    boolean existsByTemplateCodeAndCompanyCompanyIdAndIsDeletedFalse(String templateCode, Long companyId);

    boolean existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse(String templateName, Long companyId);

    boolean existsByTemplateCodeAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
            String templateCode, Long templateId, Long companyId);

    boolean existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse(
            String templateName, Long templateId, Long companyId);

    @Query("SELECT COUNT(t) FROM Template t WHERE t.company.companyId = :companyId AND t.isDeleted = false")
    long countByCompanyCompanyId(@Param("companyId") Long companyId);
}
