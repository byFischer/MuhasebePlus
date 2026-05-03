package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardWidgetRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetReorderRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardWidgetResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.repository.DashboardLayoutRepository;
import com.MuhasebePlus.demo.dashboard.repository.DashboardWidgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DashboardWidgetService {

    @Autowired private DashboardWidgetRepository widgetRepository;
    @Autowired private DashboardLayoutRepository layoutRepository;
    @Autowired private CompanyContext companyContext;

    public List<DashboardWidgetResponseDto> getWidgets(Long layoutId) {
        verifyLayoutOwnership(layoutId);
        return widgetRepository.findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(layoutId)
                .stream().map(this::toDto).toList();
    }

    public DashboardWidgetResponseDto addWidget(Long layoutId, DashboardWidgetRequestDto dto) {
        DashboardLayout layout = verifyLayoutOwnership(layoutId);

        DashboardWidget widget = new DashboardWidget();
        widget.setLayout(layout);
        widget.setWidgetType(dto.widgetType());
        widget.setTitle(dto.title());
        widget.setPositionX(dto.positionX());
        widget.setPositionY(dto.positionY());
        widget.setWidth(dto.width() > 0 ? dto.width() : 4);
        widget.setHeight(dto.height() > 0 ? dto.height() : 3);
        widget.setConfig(dto.config());

        return toDto(widgetRepository.save(widget));
    }

    public DashboardWidgetResponseDto updateWidget(Long layoutId, Long widgetId, DashboardWidgetRequestDto dto) {
        verifyLayoutOwnership(layoutId);
        DashboardWidget widget = widgetRepository.findByWidgetIdAndLayoutLayoutId(widgetId, layoutId)
                .orElseThrow(() -> new RuntimeException("Widget not found: " + widgetId));

        widget.setWidgetType(dto.widgetType());
        widget.setTitle(dto.title());
        widget.setPositionX(dto.positionX());
        widget.setPositionY(dto.positionY());
        widget.setWidth(dto.width() > 0 ? dto.width() : widget.getWidth());
        widget.setHeight(dto.height() > 0 ? dto.height() : widget.getHeight());
        if (dto.config() != null) widget.setConfig(dto.config());

        return toDto(widgetRepository.save(widget));
    }

    public void deleteWidget(Long layoutId, Long widgetId) {
        verifyLayoutOwnership(layoutId);
        DashboardWidget widget = widgetRepository.findByWidgetIdAndLayoutLayoutId(widgetId, layoutId)
                .orElseThrow(() -> new RuntimeException("Widget not found: " + widgetId));
        widgetRepository.delete(widget);
    }

    public void reorderWidgets(Long layoutId, WidgetReorderRequestDto dto) {
        verifyLayoutOwnership(layoutId);
        List<Long> ids = dto.orderedWidgetIds();
        for (int i = 0; i < ids.size(); i++) {
            final int index = i;
            widgetRepository.findByWidgetIdAndLayoutLayoutId(ids.get(i), layoutId)
                    .ifPresent(w -> {
                        w.setPositionY(index);
                        widgetRepository.save(w);
                    });
        }
    }

    private DashboardLayout verifyLayoutOwnership(Long layoutId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return layoutRepository.findByLayoutIdAndCompanyCompanyIdAndIsDeletedFalse(layoutId, companyId)
                .orElseThrow(() -> new RuntimeException("Layout not found or access denied: " + layoutId));
    }

    private DashboardWidgetResponseDto toDto(DashboardWidget w) {
        return new DashboardWidgetResponseDto(
                w.getWidgetId(),
                w.getWidgetType(),
                w.getTitle(),
                w.getPositionX(),
                w.getPositionY(),
                w.getWidth(),
                w.getHeight(),
                w.getConfig(),
                w.getCreatedAt()
        );
    }
}
