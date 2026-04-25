package com.MuhasebePlus.demo.financial.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.financial.dto.request.TransactionTemplateRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionTemplateResponseDto;
import com.MuhasebePlus.demo.financial.service.TransactionTemplateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transaction-templates")
public class TransactionTemplateController {

    @Autowired
    private TransactionTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionTemplateResponseDto> createTemplate(
            @Valid @RequestBody TransactionTemplateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<TransactionTemplateResponseDto>> getAllTemplates(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(templateService.getTemplatesByType(type));
        }
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionTemplateResponseDto> getTemplateById(@PathVariable Long templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<TransactionTemplateResponseDto> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody TransactionTemplateRequestDto dto) {
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
    public ResponseEntity<TransactionTemplateResponseDto> restoreTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(templateService.restoreTemplate(templateId));
    }
}
