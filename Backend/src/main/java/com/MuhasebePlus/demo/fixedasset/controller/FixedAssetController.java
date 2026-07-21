package com.MuhasebePlus.demo.fixedasset.controller;

import com.MuhasebePlus.demo.fixedasset.dto.*;
import com.MuhasebePlus.demo.fixedasset.service.DepreciationService;
import com.MuhasebePlus.demo.fixedasset.service.FixedAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService assetService;
    private final DepreciationService depreciationService;

    // --- Categories ---

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<AssetCategoryDto>> listCategories() {
        return ResponseEntity.ok(assetService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetCategoryDto> createCategory(@RequestBody AssetCategoryDto dto) {
        return ResponseEntity.ok(assetService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetCategoryDto> updateCategory(@PathVariable Long id, @RequestBody AssetCategoryDto dto) {
        return ResponseEntity.ok(assetService.updateCategory(id, dto));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        assetService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // --- Assets ---

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<FixedAssetResponseDto>> listAssets() {
        return ResponseEntity.ok(assetService.listAssets());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FixedAssetResponseDto> getAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAsset(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FixedAssetResponseDto> createAsset(@RequestBody FixedAssetRequestDto dto) {
        return ResponseEntity.ok(assetService.createAsset(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FixedAssetResponseDto> updateAsset(@PathVariable Long id, @RequestBody FixedAssetRequestDto dto) {
        return ResponseEntity.ok(assetService.updateAsset(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DepreciationLineDto>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getSchedule(id));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FixedAssetResponseDto> disposeAsset(@PathVariable Long id,
                                                               @RequestBody DisposeAssetRequestDto dto) {
        return ResponseEntity.ok(depreciationService.disposeAsset(id, dto));
    }

    // --- Depreciation ---

    @PostMapping("/depreciation/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> runDepreciation(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(depreciationService.runMonthlyDepreciation(year, month));
    }
}
