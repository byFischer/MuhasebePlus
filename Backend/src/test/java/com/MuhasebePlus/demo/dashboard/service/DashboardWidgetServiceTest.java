package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardWidgetRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetReorderRequestDto;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
import com.MuhasebePlus.demo.dashboard.repository.DashboardWidgetRepository;
import com.MuhasebePlus.demo.dashboard.repository.WidgetDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardWidgetServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long LAYOUT_ID = 20L;

    @Mock private DashboardWidgetRepository widgetRepository;
    @Mock private DashboardLayoutRepository layoutRepository;
    @Mock private WidgetDefinitionRepository definitionRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks private DashboardWidgetService service;

    private DashboardLayout layout;

    @BeforeEach
    void setUp() {
        layout = new DashboardLayout();
        layout.setLayoutId(LAYOUT_ID);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void getWidgets_verifiesOwnershipAndMapsDefinitions() {
        DashboardWidget widget = widget(1L, 2);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(LAYOUT_ID))
                .thenReturn(List.of(widget));

        var result = service.getWidgets(LAYOUT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().definitionId()).isEqualTo(100L);
        assertThat(result.getFirst().visualConfig()).containsEntry("chartType", "KPI");
    }

    @Test
    void addWidget_defaultsSizeAndAttachesDefinition() {
        WidgetDefinition definition = definition();
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(LAYOUT_ID))
                .thenReturn(List.of());
        when(definitionRepository.findById(100L)).thenReturn(Optional.of(definition));
        when(widgetRepository.save(any(DashboardWidget.class))).thenAnswer(inv -> {
            DashboardWidget saved = inv.getArgument(0);
            saved.setWidgetId(30L);
            return saved;
        });

        var result = service.addWidget(
                LAYOUT_ID,
                new DashboardWidgetRequestDto(WidgetType.DATA_KPI, 100L, "KPI", 5, 1, 2, 0, -1, "{}"));

        assertThat(result.widgetId()).isEqualTo(30L);
        assertThat(result.width()).isEqualTo(4);
        assertThat(result.height()).isEqualTo(3);
        assertThat(result.dataSource()).isEqualTo("INVOICE");
    }

    @Test
    void addWidget_whenSlotOccupied_throwsBeforeSave() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(LAYOUT_ID))
                .thenReturn(List.of(widget(1L, 2)));

        assertThatThrownBy(() -> service.addWidget(
                LAYOUT_ID,
                new DashboardWidgetRequestDto(WidgetType.KPI, null, "KPI", 2, 0, 0, 4, 3, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Slot dolu: 2");

        verify(widgetRepository, never()).save(any());
    }

    @Test
    void addWidget_whenDefinitionMissing_throwsRuntimeException() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(LAYOUT_ID))
                .thenReturn(List.of());
        when(definitionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addWidget(
                LAYOUT_ID,
                new DashboardWidgetRequestDto(WidgetType.DATA_CHART, 404L, "Grafik", 9, 0, 0, 6, 4, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Widget definition not found: 404");
    }

    @Test
    void updateWidget_keepsExistingSizeAndConfigWhenDtoDoesNotOverride() {
        DashboardWidget existing = widget(5L, 1);
        existing.setWidth(8);
        existing.setHeight(4);
        existing.setConfig("{\"old\":true}");
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(5L, LAYOUT_ID))
                .thenReturn(Optional.of(existing));
        when(definitionRepository.findById(100L)).thenReturn(Optional.of(definition()));
        when(widgetRepository.save(any(DashboardWidget.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateWidget(
                LAYOUT_ID,
                5L,
                new DashboardWidgetRequestDto(WidgetType.DATA_TABLE, 100L, "Tablo", null, 3, 4, 0, 0, null));

        assertThat(result.widgetType()).isEqualTo(WidgetType.DATA_TABLE);
        assertThat(result.slotIndex()).isEqualTo(1);
        assertThat(result.width()).isEqualTo(8);
        assertThat(result.height()).isEqualTo(4);
        assertThat(result.config()).isEqualTo("{\"old\":true}");
    }

    @Test
    void updateWidget_whenWidgetMissing_throwsRuntimeException() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(77L, LAYOUT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateWidget(
                LAYOUT_ID,
                77L,
                new DashboardWidgetRequestDto(WidgetType.KPI, null, "KPI", null, 0, 0, 4, 3, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Widget not found: 77");
    }

    @Test
    void deleteWidget_deletesOwnedWidget() {
        DashboardWidget existing = widget(6L, 1);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(6L, LAYOUT_ID))
                .thenReturn(Optional.of(existing));

        service.deleteWidget(LAYOUT_ID, 6L);

        verify(widgetRepository).delete(existing);
    }

    @Test
    void reorderWidgets_updatesPositionForFoundWidgetsOnly() {
        DashboardWidget first = widget(1L, 1);
        DashboardWidget third = widget(3L, 3);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.of(layout));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(1L, LAYOUT_ID)).thenReturn(Optional.of(first));
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(2L, LAYOUT_ID)).thenReturn(Optional.empty());
        when(widgetRepository.findByWidgetIdAndLayoutLayoutId(3L, LAYOUT_ID)).thenReturn(Optional.of(third));

        service.reorderWidgets(LAYOUT_ID, new WidgetReorderRequestDto(List.of(1L, 2L, 3L)));

        assertThat(first.getPositionY()).isZero();
        assertThat(third.getPositionY()).isEqualTo(2);
        verify(widgetRepository).save(first);
        verify(widgetRepository).save(third);
    }

    @Test
    void getWidgets_whenLayoutMissing_throwsAccessDenied() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(LAYOUT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWidgets(LAYOUT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Layout not found or access denied");
    }

    private DashboardWidget widget(Long id, Integer slotIndex) {
        DashboardWidget widget = new DashboardWidget();
        widget.setWidgetId(id);
        widget.setLayout(layout);
        widget.setDefinition(definition());
        widget.setWidgetType(WidgetType.KPI);
        widget.setTitle("Gelir");
        widget.setSlotIndex(slotIndex);
        widget.setPositionX(1);
        widget.setPositionY(1);
        widget.setWidth(4);
        widget.setHeight(3);
        widget.setConfig("{\"metric\":\"total_income\"}");
        return widget;
    }

    private WidgetDefinition definition() {
        WidgetDefinition definition = new WidgetDefinition();
        definition.setDefinitionId(100L);
        definition.setName("Def");
        definition.setDataSource("INVOICE");
        definition.setQueryConfig(Map.of("dataSource", "INVOICE"));
        definition.setVisualConfig(Map.of("chartType", "KPI"));
        return definition;
    }
}
