package com.MuhasebePlus.demo.declaration.controller;

import com.MuhasebePlus.demo.declaration.dto.WithholdingDeclarationLineDto;
import com.MuhasebePlus.demo.declaration.dto.WithholdingDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclaration;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclarationLine;
import com.MuhasebePlus.demo.declaration.service.WithholdingDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/declarations/withholding")
@RequiredArgsConstructor
public class WithholdingDeclarationController {

    private final WithholdingDeclarationService service;

    @PostMapping("/generate")
    public ResponseEntity<WithholdingDeclarationResponseDto> generate(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(toDto(service.generate(year, month)));
    }

    @GetMapping
    public ResponseEntity<List<WithholdingDeclarationResponseDto>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WithholdingDeclarationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.getById(id)));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<WithholdingDeclarationResponseDto> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.finalize(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private WithholdingDeclarationResponseDto toDto(WithholdingDeclaration d) {
        List<WithholdingDeclarationLineDto> lines = d.getLines().stream()
                .map(this::toLineDto)
                .toList();
        return new WithholdingDeclarationResponseDto(
                d.getDeclarationId(),
                d.getYear(),
                d.getMonth(),
                d.getStatus(),
                d.getTotalWithholdingBase(),
                d.getTotalWithholdingAmount(),
                lines
        );
    }

    private WithholdingDeclarationLineDto toLineDto(WithholdingDeclarationLine l) {
        return new WithholdingDeclarationLineDto(
                l.getLineId(),
                l.getWithholdingTaxCodeId(),
                l.getWithholdingCode(),
                l.getDescription(),
                l.getBaseAmount(),
                l.getWithholdingAmount(),
                l.getDocumentCount(),
                l.getLineOrder()
        );
    }
}
