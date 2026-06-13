package com.MuhasebePlus.demo.fixedasset.controller;

import com.MuhasebePlus.demo.fixedasset.dto.AssetCategoryDto;
import com.MuhasebePlus.demo.fixedasset.dto.DepreciationLineDto;
import com.MuhasebePlus.demo.fixedasset.dto.DisposeAssetRequestDto;
import com.MuhasebePlus.demo.fixedasset.dto.FixedAssetRequestDto;
import com.MuhasebePlus.demo.fixedasset.dto.FixedAssetResponseDto;
import com.MuhasebePlus.demo.fixedasset.service.DepreciationService;
import com.MuhasebePlus.demo.fixedasset.service.FixedAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FixedAssetController için birim testleri. Servisler mock'lanır; her endpoint'in
 * doğru servise yönlendirdiği ve durum kodu döndürdüğü doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class FixedAssetControllerUnitTest {

    @Mock private FixedAssetService assetService;
    @Mock private DepreciationService depreciationService;

    @InjectMocks
    private FixedAssetController controller;

    private AssetCategoryDto categoryDto() {
        return new AssetCategoryDto(1L, "Kat", "STRAIGHT_LINE", 60, null, null, null);
    }

    private FixedAssetResponseDto assetDto(Long id) {
        return new FixedAssetResponseDto(id, "Asset", null, null, null, null, null,
                LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "ACTIVE", null);
    }

    @Test
    void listCategories_delegates() {
        when(assetService.listCategories()).thenReturn(List.of(categoryDto()));
        ResponseEntity<List<AssetCategoryDto>> response = controller.listCategories();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void createCategory_delegates() {
        AssetCategoryDto dto = categoryDto();
        when(assetService.createCategory(dto)).thenReturn(dto);
        ResponseEntity<AssetCategoryDto> response = controller.createCategory(dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void updateCategory_delegates() {
        AssetCategoryDto dto = categoryDto();
        when(assetService.updateCategory(1L, dto)).thenReturn(dto);
        ResponseEntity<AssetCategoryDto> response = controller.updateCategory(1L, dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteCategory_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteCategory(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(assetService).deleteCategory(1L);
    }

    @Test
    void listAssets_delegates() {
        when(assetService.listAssets()).thenReturn(List.of(assetDto(1L)));
        ResponseEntity<List<FixedAssetResponseDto>> response = controller.listAssets();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getAsset_delegates() {
        when(assetService.getAsset(1L)).thenReturn(assetDto(1L));
        ResponseEntity<FixedAssetResponseDto> response = controller.getAsset(1L);
        assertThat(response.getBody().id()).isEqualTo(1L);
    }

    @Test
    void createAsset_delegates() {
        FixedAssetRequestDto req = new FixedAssetRequestDto(null, "A", null, LocalDate.now(), BigDecimal.TEN, null);
        when(assetService.createAsset(req)).thenReturn(assetDto(2L));
        ResponseEntity<FixedAssetResponseDto> response = controller.createAsset(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(2L);
    }

    @Test
    void updateAsset_delegates() {
        FixedAssetRequestDto req = new FixedAssetRequestDto(null, "A", null, LocalDate.now(), BigDecimal.TEN, null);
        when(assetService.updateAsset(2L, req)).thenReturn(assetDto(2L));
        ResponseEntity<FixedAssetResponseDto> response = controller.updateAsset(2L, req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteAsset_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteAsset(2L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(assetService).deleteAsset(2L);
    }

    @Test
    void getSchedule_delegates() {
        DepreciationLineDto line = new DepreciationLineDto(1L, 2026, 1,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null);
        when(assetService.getSchedule(2L)).thenReturn(List.of(line));
        ResponseEntity<List<DepreciationLineDto>> response = controller.getSchedule(2L);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void disposeAsset_delegatesToDepreciationService() {
        DisposeAssetRequestDto req = new DisposeAssetRequestDto(LocalDate.now(), BigDecimal.ZERO, null);
        when(depreciationService.disposeAsset(2L, req)).thenReturn(assetDto(2L));
        ResponseEntity<FixedAssetResponseDto> response = controller.disposeAsset(2L, req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void runDepreciation_delegatesToDepreciationService() {
        Map<String, Object> summary = Map.of("processed", 3, "skipped", 1, "totalAmount", BigDecimal.TEN);
        when(depreciationService.runMonthlyDepreciation(2026, 2)).thenReturn(summary);
        ResponseEntity<Map<String, Object>> response = controller.runDepreciation(2026, 2);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("processed", 3);
    }
}
