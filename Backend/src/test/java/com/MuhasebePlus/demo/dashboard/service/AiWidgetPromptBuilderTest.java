package com.MuhasebePlus.demo.dashboard.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiWidgetPromptBuilderTest {

    private final AiWidgetPromptBuilder builder = new AiWidgetPromptBuilder();

    @Test
    void buildSystemInstruction_includesCurrentDatesAndDashboardRules() {
        String instruction = builder.buildSystemInstruction();

        assertThat(instruction)
                .contains(LocalDate.now().toString())
                .contains("DATA_KPI")
                .contains("INVOICE")
                .contains("company_id")
                .contains("GT/GTE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildResponseSchema_containsNestedWidgetDefinitionSchema() {
        Map<String, Object> schema = builder.buildResponseSchema();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> widgetDefinition = (Map<String, Object>) properties.get("widgetDefinition");
        Map<String, Object> widgetProperties = (Map<String, Object>) widgetDefinition.get("properties");
        Map<String, Object> queryConfig = (Map<String, Object>) widgetProperties.get("queryConfig");
        Map<String, Object> visualConfig = (Map<String, Object>) widgetProperties.get("visualConfig");

        assertThat(schema).containsEntry("type", "OBJECT");
        assertThat(properties).containsKeys("assistantMessage", "widgetDefinition");
        assertThat(widgetDefinition).containsEntry("nullable", true);
        assertThat(queryConfig.get("properties")).asString().contains("filters", "aggregate", "limit");
        assertThat(visualConfig.get("properties")).asString().contains("chartType", "color");
    }
}
