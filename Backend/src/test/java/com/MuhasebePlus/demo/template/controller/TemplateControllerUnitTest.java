package com.MuhasebePlus.demo.template.controller;

import com.MuhasebePlus.demo.template.dto.request.TemplateApplyRequestDto;
import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateApplyResponseDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.service.TemplateApplicationService;
import com.MuhasebePlus.demo.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TemplateController için birim testleri. İki servis mock'lanır; controller'ın
 * yönlendirme, durum kodu ve query-param'a göre dallanma davranışı doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class TemplateControllerUnitTest {

    @Mock private TemplateService templateService;
    @Mock private TemplateApplicationService templateApplicationService;

    @InjectMocks
    private TemplateController controller;

    private TemplateResponseDto dto(Long id) {
        return new TemplateResponseDto(id, "TPL", "Ad", TemplateType.EXPENSE,
                "desc", "MONTHLY", null, 0L, null, null);
    }

    @Test
    void getTemplates_noType_returnsAll() {
        when(templateService.getAllTemplates()).thenReturn(List.of(dto(1L)));

        ResponseEntity<List<TemplateResponseDto>> response = controller.getTemplates(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getTemplates_blankType_returnsAll() {
        when(templateService.getAllTemplates()).thenReturn(List.of());

        ResponseEntity<List<TemplateResponseDto>> response = controller.getTemplates("   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getTemplates_withType_filtersByType() {
        when(templateService.getTemplatesByType("INCOME")).thenReturn(List.of(dto(2L)));

        ResponseEntity<List<TemplateResponseDto>> response = controller.getTemplates("INCOME");

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).templateId()).isEqualTo(2L);
    }

    @Test
    void getTemplateById_delegates() {
        when(templateService.getTemplateById(5L)).thenReturn(dto(5L));

        ResponseEntity<TemplateResponseDto> response = controller.getTemplateById(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().templateId()).isEqualTo(5L);
    }

    @Test
    void createTemplate_returnsCreated() {
        TemplateRequestDto req = new TemplateRequestDto("TPL", "Ad", TemplateType.EXPENSE, null, null, null);
        when(templateService.createTemplate(req)).thenReturn(dto(10L));

        ResponseEntity<TemplateResponseDto> response = controller.createTemplate(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().templateId()).isEqualTo(10L);
    }

    @Test
    void updateTemplate_returnsOk() {
        TemplateRequestDto req = new TemplateRequestDto("TPL", "Ad", TemplateType.EXPENSE, null, null, null);
        when(templateService.updateTemplate(5L, req)).thenReturn(dto(5L));

        ResponseEntity<TemplateResponseDto> response = controller.updateTemplate(5L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().templateId()).isEqualTo(5L);
    }

    @Test
    void deleteTemplate_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteTemplate(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(templateService).softDeleteTemplate(5L);
        verifyNoInteractions(templateApplicationService);
    }

    @Test
    void restoreTemplate_returnsOk() {
        when(templateService.restoreTemplate(5L)).thenReturn(dto(5L));

        ResponseEntity<TemplateResponseDto> response = controller.restoreTemplate(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void applyTemplate_delegatesToApplicationService() {
        TemplateApplyRequestDto req = new TemplateApplyRequestDto("not");
        TemplateApplyResponseDto resp = new TemplateApplyResponseDto(true, "ok", "Transaction", 7L);
        when(templateApplicationService.applyTemplate(5L, req)).thenReturn(resp);

        ResponseEntity<TemplateApplyResponseDto> response = controller.applyTemplate(5L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().targetEntityId()).isEqualTo(7L);
    }
}
