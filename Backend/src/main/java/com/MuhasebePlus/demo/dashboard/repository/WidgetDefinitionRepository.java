package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WidgetDefinitionRepository extends JpaRepository<WidgetDefinition, Long> {

    List<WidgetDefinition> findByCompanyCompanyId(Long companyId);

    List<WidgetDefinition> findByCompanyCompanyIdAndIsTemplateTrue(Long companyId);

    List<WidgetDefinition> findByCompanyCompanyIdAndUserUserId(Long companyId, Long userId);

    List<WidgetDefinition> findByIsSystemTrue();

    List<WidgetDefinition> findByCompanyCompanyIdAndWidgetType(Long companyId, String widgetType);

    List<WidgetDefinition> findByCompanyCompanyIdOrIsSystemTrue(Long companyId);
}
