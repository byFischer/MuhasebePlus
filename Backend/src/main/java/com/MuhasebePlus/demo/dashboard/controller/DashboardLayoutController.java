package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.dashboard.dto.request.DashboardLayoutRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardLayoutResponseDto;
import com.MuhasebePlus.demo.dashboard.service.DashboardLayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/layouts")
@RequiredArgsConstructor
public class DashboardLayoutController {

    private final DashboardLayoutService layoutService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<DashboardLayoutResponseDto>> getLayouts() {
        return ResponseEntity.ok(layoutService.getLayouts());
    }

    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardLayoutResponseDto> getDefaultLayout() {
        return ResponseEntity.ok(layoutService.getDefaultLayout());
    }

    @GetMapping("/{layoutId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardLayoutResponseDto> getLayout(@PathVariable Long layoutId) {
        return ResponseEntity.ok(layoutService.getLayout(layoutId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardLayoutResponseDto> createLayout(@Valid @RequestBody DashboardLayoutRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(layoutService.createLayout(dto));
    }

    @PutMapping("/{layoutId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardLayoutResponseDto> updateLayout(
            @PathVariable Long layoutId,
            @Valid @RequestBody DashboardLayoutRequestDto dto) {
        return ResponseEntity.ok(layoutService.updateLayout(layoutId, dto));
    }

    @DeleteMapping("/{layoutId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> deleteLayout(@PathVariable Long layoutId) {
        layoutService.deleteLayout(layoutId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{layoutId}/clone")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<DashboardLayoutResponseDto> cloneLayout(@PathVariable Long layoutId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(layoutService.cloneLayout(layoutId));
    }
}
