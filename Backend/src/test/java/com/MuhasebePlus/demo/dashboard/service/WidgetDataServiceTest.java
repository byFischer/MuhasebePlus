package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
import com.MuhasebePlus.demo.dashboard.repository.DashboardWidgetRepository;
import com.MuhasebePlus.demo.dashboard.service.provider.BuiltInWidgetDataProvider;
import com.MuhasebePlus.demo.dashboard.service.provider.DataQueryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetDataServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long LAYOUT_ID = 5L;
    private static final Long WIDGET_ID = 9L;

    @Mock private DashboardWidgetRepository widgetRepository;
    @Mock private DashboardLayoutRepository layoutRepository;
    @Mock private CompanyContext companyContext;
    @Mock private BuiltInWidgetDataProvider builtInProvider;
    @Mock private DataQueryProvider dataQueryProvider;

    @InjectMocks private WidgetDataService service;

    @BeforeEach
    void setUp() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void getWidgetData_usesBuiltInProviderAndMergesDefinitionMetadata() {
        DashboardWidget widget = widget(WidgetType.KPI, "{\"metric\":\"total_expense\",\"queryConfig\":{\"ignored\":true}}");
        widget.setTitle(null);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(new DashboardLayout()));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(WIDGET_ID, LAYOUT_ID))
                .thenReturn(Optional.of(widget));
        when(builtInProvider.supports(WidgetType.KPI)).thenReturn(true);
        when(builtInProvider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of(
                "queryConfig", Map.of("dataSource", "INVOICE"),
                "visualConfig", Map.of("chartType", "KPI"),
                "metric", "total_income")))
                .thenReturn(Map.of("value", 100));

        Map<String, Object> result = service.getWidgetData(LAYOUT_ID, WIDGET_ID);

        assertThat(result)
                .containsEntry("value", 100)
                .containsEntry("title", "Def")
                .containsEntry("visualConfig", Map.of("chartType", "KPI"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getWidgetData_usesDataQueryProviderWithWidgetConfigFallbacks() {
        DashboardWidget widget = widget(WidgetType.DATA_TABLE, "{\"limit\":5,\"visualConfig\":{\"chartType\":\"TABLE\"}}");
        widget.setDefinition(null);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(new DashboardLayout()));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(WIDGET_ID, LAYOUT_ID))
                .thenReturn(Optional.of(widget));
        when(builtInProvider.supports(WidgetType.DATA_TABLE)).thenReturn(false);
        when(dataQueryProvider.supports(WidgetType.DATA_TABLE)).thenReturn(true);
        when(dataQueryProvider.fetchData(org.mockito.ArgumentMatchers.eq(COMPANY_ID), anyMap()))
                .thenReturn(Map.of("data", java.util.List.of()));

        Map<String, Object> result = service.getWidgetData(LAYOUT_ID, WIDGET_ID);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(dataQueryProvider).fetchData(org.mockito.ArgumentMatchers.eq(COMPANY_ID), captor.capture());
        assertThat(captor.getValue()).containsEntry("limit", 5);
        assertThat(captor.getValue()).containsEntry("visualConfig", Map.of("chartType", "TABLE"));
        assertThat(result).containsKey("data");
    }

    @Test
    void getWidgetData_whenProviderDoesNotSupportType_returnsMessageAndIgnoresInvalidJson() {
        DashboardWidget widget = widget(WidgetType.FORECAST, "{broken");
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(new DashboardLayout()));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(WIDGET_ID, LAYOUT_ID))
                .thenReturn(Optional.of(widget));
        when(builtInProvider.supports(WidgetType.FORECAST)).thenReturn(false);
        when(dataQueryProvider.supports(WidgetType.FORECAST)).thenReturn(false);

        Map<String, Object> result = service.getWidgetData(LAYOUT_ID, WIDGET_ID);

        assertThat(result.get("message")).asString().contains("FORECAST");
        assertThat(result).containsEntry("title", "Widget title");
    }

    @Test
    void getWidgetData_whenLayoutMissing_throwsRuntimeException() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWidgetData(LAYOUT_ID, WIDGET_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Layout not found or access denied");
    }

    @Test
    void getWidgetData_whenWidgetMissing_throwsRuntimeException() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(new DashboardLayout()));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(WIDGET_ID, LAYOUT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWidgetData(LAYOUT_ID, WIDGET_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Widget not found: 9");
    }

    private DashboardWidget widget(WidgetType type, String config) {
        WidgetDefinition definition = new WidgetDefinition();
        definition.setDefinitionId(7L);
        definition.setName("Def");
        definition.setDataSource("INVOICE");
        definition.setQueryConfig(Map.of("dataSource", "INVOICE"));
        definition.setVisualConfig(Map.of("chartType", "KPI"));
        definition.setConfig(Map.of("metric", "total_income"));

        DashboardWidget widget = new DashboardWidget();
        widget.setWidgetId(WIDGET_ID);
        widget.setWidgetType(type);
        widget.setTitle("Widget title");
        widget.setDefinition(definition);
        widget.setConfig(config);
        return widget;
    }
}
