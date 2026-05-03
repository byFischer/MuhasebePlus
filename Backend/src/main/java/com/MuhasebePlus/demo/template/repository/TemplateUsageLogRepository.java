package com.MuhasebePlus.demo.template.repository;

import com.MuhasebePlus.demo.template.entity.TemplateUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateUsageLogRepository extends JpaRepository<TemplateUsageLog, Long> {

    long countByTemplateId(Long templateId);

    List<TemplateUsageLog> findByTemplateIdOrderByUsedAtDesc(Long templateId);

    @Query("SELECT l.templateId, COUNT(l) FROM TemplateUsageLog l WHERE l.templateId IN :templateIds GROUP BY l.templateId")
    List<Object[]> countByTemplateIds(@Param("templateIds") List<Long> templateIds);
}
