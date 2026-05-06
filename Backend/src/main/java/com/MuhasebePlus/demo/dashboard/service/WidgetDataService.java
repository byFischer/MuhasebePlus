package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
import com.MuhasebePlus.demo.dashboard.repository.DashboardWidgetRepository;
import com.MuhasebePlus.demo.dashboard.service.provider.BuiltInWidgetDataProvider;
import com.MuhasebePlus.demo.dashboard.service.provider.DataQueryProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WidgetDataService {

    @Autowired private DashboardWidgetRepository widgetRepository;
    @Autowired private DashboardLayoutRepository layoutRepository;
    @Autowired private CompanyContext companyContext;
    @Autowired private BuiltInWidgetDataProvider builtInProvider;
    @Autowired private DataQueryProvider dataQueryProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getWidgetData(Long layoutId, Long widgetId) {
        Long companyId = companyContext.getCurrentCompanyId();

        layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found or access denied"));

        DashboardWidget widget = widgetRepository.findByWidgetIdAndLayoutLayoutId(widgetId, layoutId)
                .orElseThrow(() -> new RuntimeException("Widget not found: " + widgetId));

        Map<String, Object> config = buildConfig(widget);

        Map<String, Object> result;
        if (builtInProvider.supports(widget.getWidgetType())) {
            result = builtInProvider.fetchData(widget.getWidgetType(), companyId, config);
        } else if (dataQueryProvider.supports(widget.getWidgetType())) {
            result = dataQueryProvider.fetchData(companyId, config);
        } else {
            result = Map.of("message", "Widget data not available for type: " + widget.getWidgetType());
        }

        Map<String, Object> response = new HashMap<>(result);
        WidgetDefinition def = widget.getDefinition();
        if (def != null) {
            if (def.getVisualConfig() != null) response.put("visualConfig", def.getVisualConfig());
            response.put("title", widget.getTitle() != null ? widget.getTitle() : def.getName());
        }
        return response;
    }

    private Map<String, Object> buildConfig(DashboardWidget widget) {
        Map<String, Object> config = new HashMap<>();

        WidgetDefinition def = widget.getDefinition();
        if (def != null) {
            if (def.getQueryConfig() != null) config.put("queryConfig", def.getQueryConfig());
            if (def.getVisualConfig() != null) config.put("visualConfig", def.getVisualConfig());
            if (def.getConfig() != null) config.putAll(def.getConfig());
        }

        Map<String, Object> widgetConfig = parseConfig(widget.getConfig());
        if (!widgetConfig.isEmpty()) {
            if (widgetConfig.containsKey("queryConfig") && !config.containsKey("queryConfig")) {
                config.put("queryConfig", widgetConfig.get("queryConfig"));
            }
            if (widgetConfig.containsKey("visualConfig") && !config.containsKey("visualConfig")) {
                config.put("visualConfig", widgetConfig.get("visualConfig"));
            }
            widgetConfig.forEach((k, v) -> {
                if (!"queryConfig".equals(k) && !"visualConfig".equals(k)) {
                    config.putIfAbsent(k, v);
                }
            });
        }
        return config;
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
