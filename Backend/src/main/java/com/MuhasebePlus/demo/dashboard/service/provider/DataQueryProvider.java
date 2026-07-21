package com.MuhasebePlus.demo.dashboard.service.provider;

import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.service.DynamicQueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataQueryProvider implements WidgetDataProvider {

    private final DynamicQueryService dynamicQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public boolean supports(WidgetType type) {
        return type == WidgetType.DATA_QUERY || type == WidgetType.DATA_CHART
                || type == WidgetType.DATA_KPI || type == WidgetType.DATA_TABLE;
    }

    @Override
    public Map<String, Object> fetchData(Long companyId, Map<String, Object> config) {
        try {
            Object raw = config.containsKey("queryConfig")
                    ? config.get("queryConfig")
                    : parseJsonRaw((String) config.get("queryConfigJson"));
            QueryConfigDto queryConfig = objectMapper.convertValue(raw, QueryConfigDto.class);
            DynamicQueryService.QueryResult result = dynamicQueryService.executeQuery(queryConfig, companyId);

            return Map.of(
                    "data", result.rows(),
                    "columns", result.columns(),
                    "aggregateMeta", result.aggregateMeta(),
                    "totalCount", result.totalCount()
            );
        } catch (Exception e) {
            return Map.of("error", true, "message", e.getMessage());
        }
    }

    private Map<String, Object> parseJsonRaw(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
