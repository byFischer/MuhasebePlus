package com.MuhasebePlus.demo.template.controller;

import com.MuhasebePlus.demo.template.dto.request.TemplateApplyRequestDto;
import com.MuhasebePlus.demo.template.dto.request.TemplateRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateApplyResponseDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateResponseDto;
import com.MuhasebePlus.demo.template.service.TemplateApplicationService;
import com.MuhasebePlus.demo.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateApplicationService templateApplicationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<TemplateResponseDto>> getTemplates(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(templateService.getTemplatesByType(type));
        }
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TemplateResponseDto> getTemplateById(@PathVariable Long templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TemplateResponseDto> createTemplate(
            @Valid @RequestBody TemplateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(dto));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TemplateResponseDto> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateRequestDto dto) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, dto));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        templateService.softDeleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{templateId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponseDto> restoreTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(templateService.restoreTemplate(templateId));
    }

    @PostMapping("/{templateId}/apply")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TemplateApplyResponseDto> applyTemplate(
            @PathVariable Long templateId,
            @RequestBody(required = false) TemplateApplyRequestDto dto) {
        return ResponseEntity.ok(templateApplicationService.applyTemplate(templateId, dto));
    }
}
