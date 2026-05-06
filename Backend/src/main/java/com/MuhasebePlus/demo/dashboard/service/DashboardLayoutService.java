package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardLayoutRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardLayoutResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardWidgetResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DashboardLayoutService {

    @Autowired private DashboardLayoutRepository layoutRepository;
    @Autowired private CompanyContext companyContext;
    @Autowired private CompanyRepository companyRepository;

    public List<DashboardLayoutResponseDto> getLayouts() {
        Long companyId = companyContext.getCurrentCompanyId();
        return layoutRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId)
                .stream().map(this::toDto).toList();
    }

    public DashboardLayoutResponseDto getDefaultLayout() {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        return layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(companyId)
                .map(this::toDto)
                .orElseGet(() -> createDefaultLayout(companyId, userId));
    }

    public DashboardLayoutResponseDto getLayout(Long layoutId) {
        Long companyId = companyContext.getCurrentCompanyId();
        DashboardLayout layout = layoutRepository
                .findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found: " + layoutId));
        return toDto(layout);
    }

    public DashboardLayoutResponseDto createLayout(DashboardLayoutRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (dto.isDefault()) {
            unsetCurrentDefault(companyId);
        }

        DashboardLayout layout = new DashboardLayout();
        layout.setCompany(company);
        layout.setUserId(userId);
        layout.setName(dto.name());
        layout.setDefault(dto.isDefault());
        layout.setTheme(dto.theme() != null ? dto.theme() : "dark");
        layout.setAccentColor(dto.accentColor() != null ? dto.accentColor() : "#2f6df6");

        return toDto(layoutRepository.save(layout));
    }

    public DashboardLayoutResponseDto updateLayout(Long layoutId, DashboardLayoutRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        DashboardLayout layout = layoutRepository
                .findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found: " + layoutId));

        if (dto.isDefault() && !layout.isDefault()) {
            unsetCurrentDefault(companyId);
        }

        layout.setName(dto.name());
        layout.setDefault(dto.isDefault());
        if (dto.theme() != null) layout.setTheme(dto.theme());
        if (dto.accentColor() != null) layout.setAccentColor(dto.accentColor());

        return toDto(layoutRepository.save(layout));
    }

    public void deleteLayout(Long layoutId) {
        Long companyId = companyContext.getCurrentCompanyId();
        DashboardLayout layout = layoutRepository
                .findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found: " + layoutId));

        layout.setDeleted(true);
        layout.setDeletedAt(LocalDateTime.now());
        layoutRepository.save(layout);
    }

    public DashboardLayoutResponseDto cloneLayout(Long layoutId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();
        DashboardLayout source = layoutRepository
                .findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found: " + layoutId));

        DashboardLayout clone = new DashboardLayout();
        clone.setCompany(source.getCompany());
        clone.setUserId(userId);
        clone.setName(source.getName() + " (Kopya)");
        clone.setDefault(false);
        clone.setTheme(source.getTheme());
        clone.setAccentColor(source.getAccentColor());

        for (DashboardWidget w : source.getWidgets()) {
            DashboardWidget wClone = new DashboardWidget();
            wClone.setLayout(clone);
            wClone.setWidgetType(w.getWidgetType());
            wClone.setTitle(w.getTitle());
            wClone.setPositionX(w.getPositionX());
            wClone.setPositionY(w.getPositionY());
            wClone.setWidth(w.getWidth());
            wClone.setHeight(w.getHeight());
            wClone.setConfig(w.getConfig());
            clone.getWidgets().add(wClone);
        }

        return toDto(layoutRepository.save(clone));
    }

    private void unsetCurrentDefault(Long companyId) {
        layoutRepository.findByCompanyCompanyIdAndIsDefaultTrueAndIsDeletedFalse(companyId)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    layoutRepository.save(existing);
                });
    }

    private DashboardLayoutResponseDto createDefaultLayout(Long companyId, Long userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        DashboardLayout layout = new DashboardLayout();
        layout.setCompany(company);
        layout.setUserId(userId);
        layout.setName("Ana Dashboard");
        layout.setDefault(true);
        layout.setTheme("dark");
        layout.setAccentColor("#2f6df6");

        return toDto(layoutRepository.save(layout));
    }

    DashboardLayoutResponseDto toDto(DashboardLayout layout) {
        List<DashboardWidgetResponseDto> widgets = layout.getWidgets().stream()
                .map(w -> {
                    var def = w.getDefinition();
                    return new DashboardWidgetResponseDto(
                            w.getWidgetId(),
                            w.getWidgetType(),
                            def != null ? def.getDefinitionId() : null,
                            w.getTitle(),
                            w.getPositionX(),
                            w.getPositionY(),
                            w.getWidth(),
                            w.getHeight(),
                            w.getConfig(),
                            def != null ? def.getDataSource() : null,
                            def != null ? def.getQueryConfig() : null,
                            def != null ? def.getVisualConfig() : null,
                            w.getCreatedAt()
                    );
                }).toList();

        return new DashboardLayoutResponseDto(
                layout.getLayoutId(),
                layout.getName(),
                layout.isDefault(),
                layout.getTheme(),
                layout.getAccentColor(),
                widgets,
                layout.getCreatedAt(),
                layout.getUpdatedAt()
        );
    }
}
