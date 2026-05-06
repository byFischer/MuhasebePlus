package com.MuhasebePlus.demo.dashboard.service.provider;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;

import java.util.Map;

public interface WidgetDataProvider {
    boolean supports(WidgetType type);
    Map<String, Object> fetchData(Long companyId, Map<String, Object> config);
}
