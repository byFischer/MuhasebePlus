package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardLayoutRequestDto;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardLayoutServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 9L;

    @Mock private DashboardLayoutRepository layoutRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks private DashboardLayoutService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.builder().companyId(COMPANY_ID).companyName("MuhasebePlus").build();
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void getLayouts_mapsLayoutAndNestedWidgetDefinition() {
        DashboardLayout layout = layout(11L, "Ana", true);
        layout.getWidgets().add(widget(100L));
        when(layoutRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(layout));

        var result = service.getLayouts();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().layoutId()).isEqualTo(11L);
        assertThat(result.getFirst().widgets()).hasSize(1);
        assertThat(result.getFirst().widgets().getFirst().dataSource()).isEqualTo("INVOICE");
    }

    @Test
    void getDefaultLayout_whenExisting_returnsIt() {
        DashboardLayout existing = layout(12L, "Mevcut", true);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.of(existing));

        var result = service.getDefaultLayout();

        assertThat(result.layoutId()).isEqualTo(12L);
        assertThat(result.name()).isEqualTo("Mevcut");
    }

    @Test
    void getDefaultLayout_whenMissing_createsDefaultLayout() {
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.empty());
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(layoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> {
            DashboardLayout layout = inv.getArgument(0);
            layout.setLayoutId(55L);
            return layout;
        });

        var result = service.getDefaultLayout();

        assertThat(result.layoutId()).isEqualTo(55L);
        assertThat(result.name()).isEqualTo("Ana Dashboard");
        assertThat(result.isDefault()).isTrue();
        assertThat(result.theme()).isEqualTo("dark");
        assertThat(result.accentColor()).isEqualTo("#2f6df6");
        assertThat(result.layoutPreset()).isEqualTo("LAYOUT_3");
    }

    @Test
    void createLayout_whenDefault_unsetsExistingDefaultAndAppliesFallbacks() {
        DashboardLayout currentDefault = layout(1L, "Eski", true);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.of(currentDefault));
        when(layoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createLayout(new DashboardLayoutRequestDto("Yeni", true, null, null, null));

        assertThat(currentDefault.isDefault()).isFalse();
        assertThat(result.name()).isEqualTo("Yeni");
        assertThat(result.theme()).isEqualTo("dark");
        assertThat(result.accentColor()).isEqualTo("#2f6df6");
        verify(layoutRepository).save(currentDefault);
    }

    @Test
    void updateLayout_whenPresetChanges_clearsWidgetsAndUnsetsDefault() {
        DashboardLayout existing = layout(20L, "Operasyon", false);
        existing.getWidgets().add(widget(10L));
        DashboardLayout currentDefault = layout(30L, "Default", true);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(20L, COMPANY_ID))
                .thenReturn(Optional.of(existing));
        when(layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.of(currentDefault));
        when(layoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateLayout(
                20L,
                new DashboardLayoutRequestDto("Guncel", true, "light", "#ff0000", "LAYOUT_6"));

        assertThat(currentDefault.isDefault()).isFalse();
        assertThat(existing.getWidgets()).isEmpty();
        assertThat(result.name()).isEqualTo("Guncel");
        assertThat(result.layoutPreset()).isEqualTo("LAYOUT_6");
    }

    @Test
    void deleteLayout_marksSoftDeleted() {
        DashboardLayout existing = layout(22L, "Sil", false);
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(22L, COMPANY_ID))
                .thenReturn(Optional.of(existing));

        service.deleteLayout(22L);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(layoutRepository).save(existing);
    }

    @Test
    void cloneLayout_copiesWidgetConfiguration() {
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        DashboardLayout source = layout(40L, "Kaynak", true);
        source.getWidgets().add(widget(400L));
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(40L, COMPANY_ID))
                .thenReturn(Optional.of(source));
        when(layoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> {
            DashboardLayout clone = inv.getArgument(0);
            clone.setLayoutId(41L);
            return clone;
        });

        var result = service.cloneLayout(40L);

        assertThat(result.layoutId()).isEqualTo(41L);
        assertThat(result.name()).isEqualTo("Kaynak (Kopya)");
        assertThat(result.isDefault()).isFalse();
        assertThat(result.widgets()).hasSize(1);
        assertThat(result.widgets().getFirst().title()).isEqualTo("Gelir");
    }

    @Test
    void getLayout_whenMissing_throwsRuntimeException() {
        when(layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLayout(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Layout not found: 99");
    }

    @Test
    void createLayout_whenCompanyMissing_throwsRuntimeException() {
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLayout(new DashboardLayoutRequestDto("Yeni", false, null, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found");
    }

    private DashboardLayout layout(Long id, String name, boolean isDefault) {
        DashboardLayout layout = new DashboardLayout();
        layout.setLayoutId(id);
        layout.setCompany(company);
        layout.setUserId(USER_ID);
        layout.setName(name);
        layout.setDefault(isDefault);
        layout.setTheme("dark");
        layout.setAccentColor("#2f6df6");
        layout.setLayoutPreset("LAYOUT_3");
        return layout;
    }

    private DashboardWidget widget(Long id) {
        WidgetDefinition definition = new WidgetDefinition();
        definition.setDefinitionId(70L);
        definition.setName("Tanım");
        definition.setDataSource("INVOICE");
        definition.setQueryConfig(Map.of("dataSource", "INVOICE"));
        definition.setVisualConfig(Map.of("chartType", "BAR"));

        DashboardWidget widget = new DashboardWidget();
        widget.setWidgetId(id);
        widget.setDefinition(definition);
        widget.setWidgetType(WidgetType.DATA_CHART);
        widget.setTitle("Gelir");
        widget.setSlotIndex(2);
        widget.setPositionX(1);
        widget.setPositionY(2);
        widget.setWidth(6);
        widget.setHeight(4);
        widget.setConfig("{\"color\":\"blue\"}");
        return widget;
    }
}
