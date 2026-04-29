package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.dashboard.dto.request.DashboardWidgetRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetReorderRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardWidgetResponseDto;
import com.MuhasebePlus.demo.dashboard.service.DashboardWidgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/layouts/{layoutId}/widgets")
public class DashboardWidgetController {

    @Autowired private DashboardWidgetService widgetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<DashboardWidgetResponseDto>> getWidgets(@PathVariable Long layoutId) {
        return ResponseEntity.ok(widgetService.getWidgets(layoutId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardWidgetResponseDto> addWidget(
            @PathVariable Long layoutId,
            @Valid @RequestBody DashboardWidgetRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(widgetService.addWidget(layoutId, dto));
    }

    @PutMapping("/{widgetId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardWidgetResponseDto> updateWidget(
            @PathVariable Long layoutId,
            @PathVariable Long widgetId,
            @Valid @RequestBody DashboardWidgetRequestDto dto) {
        return ResponseEntity.ok(widgetService.updateWidget(layoutId, widgetId, dto));
    }

    @DeleteMapping("/{widgetId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> deleteWidget(@PathVariable Long layoutId, @PathVariable Long widgetId) {
        widgetService.deleteWidget(layoutId, widgetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> reorderWidgets(
            @PathVariable Long layoutId,
            @RequestBody WidgetReorderRequestDto dto) {
        widgetService.reorderWidgets(layoutId, dto);
        return ResponseEntity.noContent().build();
    }
}
