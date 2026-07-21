package com.MuhasebePlus.demo.declaration.controller;

import com.MuhasebePlus.demo.declaration.dto.GenericDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.GenericDeclaration;
import com.MuhasebePlus.demo.declaration.service.GenericDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/declarations/generic")
@RequiredArgsConstructor
public class GenericDeclarationController {

    private final GenericDeclarationService service;

    @PostMapping("/generate")
    public ResponseEntity<GenericDeclarationResponseDto> generate(
            @RequestParam String type,
            @RequestParam int year,
            @RequestParam int period) {
        return ResponseEntity.ok(toDto(service.generate(type, year, period)));
    }

    @GetMapping
    public ResponseEntity<List<GenericDeclarationResponseDto>> list(@RequestParam String type) {
        return ResponseEntity.ok(service.list(type).stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericDeclarationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.getById(id)));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<GenericDeclarationResponseDto> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.finalize(id)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<GenericDeclarationResponseDto> markSubmitted(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.markSubmitted(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private GenericDeclarationResponseDto toDto(GenericDeclaration d) {
        return new GenericDeclarationResponseDto(
                d.getDeclarationId(),
                d.getDeclarationType(),
                d.getYear(),
                d.getPeriod(),
                d.getStatus(),
                d.getTotalPayable(),
                d.getDataSnapshot()
        );
    }
}
